package org.apache.maven.plugin.surefire;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.surefire.booter.KeyValueSource;

/**
 * A properties implementation that preserves insertion order.
 */
public class SurefireProperties
    extends Properties implements KeyValueSource
{
    private final LinkedHashSet<Object> items = new LinkedHashSet<Object>();

    public SurefireProperties()
    {
    }

    public SurefireProperties( Properties source )
    {
        if ( source != null )
        {
            this.putAll( source );
        }
    }

    @Override
    public synchronized Object put( Object key, Object value )
    {
        items.add( key );
        return super.put( key, value );
    }

    @Override
    public synchronized Object remove( Object key )
    {
        items.remove( key );
        return super.remove( key );
    }

    @Override
    public synchronized void clear()
    {
        items.clear();
        super.clear();
    }

    public synchronized Enumeration<Object> keys()
    {
        return Collections.enumeration( items );
    }

    public void copyProperties( Properties source )
    {
        if ( source != null )
        {
            //noinspection unchecked
            for ( Object key : source.keySet() )
            {
                Object value = source.get( key );
                put( key, value );
            }
        }
    }

    private Iterable<Object> getStringKeySet()
    {

        //noinspection unchecked
        return keySet();
    }

    public void showToLog( org.apache.maven.plugin.logging.Log log, String setting )
    {
        for ( Object key : getStringKeySet() )
        {
            String value = getProperty( (String) key );
            log.debug( "Setting " + setting + " [" + key + "]=[" + value + "]" );
        }
    }

    public void verifyLegalSystemProperties( org.apache.maven.plugin.logging.Log log )
    {
        for ( Object key : getStringKeySet() )
        {
            if ( "java.library.path".equals( key ) )
            {
                log.warn(
                    "java.library.path cannot be set as system property, use <argLine>-Djava.library.path=...<argLine> instead" );
            }
        }
    }


    public void copyToSystemProperties()
    {

        //noinspection unchecked
        for ( Object o : items )
        {
            String key = (String) o;
            String value = getProperty( key );

            System.setProperty( key, value );
        }
    }

    static SurefireProperties calculateEffectiveProperties( Properties systemProperties, File systemPropertiesFile,
                                                            Map<String, String> systemPropertyVariables,
                                                            Properties userProperties, Log log )
    {
        SurefireProperties result = new SurefireProperties();
        result.copyProperties( systemProperties );

        if ( systemPropertiesFile != null )
        {
            Properties props = new SurefireProperties();
            try
            {
                InputStream fis = new FileInputStream( systemPropertiesFile );
                props.load( fis );
                fis.close();
            }
            catch ( IOException e )
            {
                String msg = "The system property file '" + systemPropertiesFile.getAbsolutePath() + "' can't be read.";
                if ( log.isDebugEnabled() )
                {
                    log.warn( msg, e );
                }
                else
                {
                    log.warn( msg );
                }
            }

            result.copyProperties( props );
        }

        copyProperties( result, systemPropertyVariables );
        copyProperties( result, systemPropertyVariables );

        // We used to take all of our system properties and dump them in with the
        // user specified properties for SUREFIRE-121, causing SUREFIRE-491.
        // Not gonna do THAT any more... instead, we only propagate those system properties
        // that have been explicitly specified by the user via -Dkey=value on the CLI

        result.copyProperties( userProperties );
        return result;
    }

    public static void copyProperties( Properties target, Map<String, String> source )
    {
        if ( source != null )
        {
            for ( String key : source.keySet() )
            {
                String value = source.get( key );
                //java Properties does not accept null value
                if ( value != null )
                {
                    target.setProperty( key, value );
                }
            }
        }
    }

    public void copyTo( Map target )
    {
        Iterator iter = keySet().iterator();
        Object key;
        while(iter.hasNext()){
            key = iter.next();
            //noinspection unchecked
            target.put(  key, get( key ));
        }
    }


}
