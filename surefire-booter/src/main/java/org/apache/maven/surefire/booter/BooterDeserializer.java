package org.apache.maven.surefire.booter;
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

import org.apache.maven.surefire.suite.SuiteDefinition;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Knows how to serialize and deserialize the booter configuration.
 * <p/>
 * The internal serialization format is through a properties file. The long-term goal of this
 * class is not to expose this implementation information to its clients. This still leaks somewhat,
 * and there are some cases where properties are being accessed as "Properties" instead of
 * more representative domain objects.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class BooterDeserializer
{
    private static final String TEST_SUITE_PROPERTY_PREFIX = "testSuite.";

    private static final String DIRSCANNER_PROPERTY_PREFIX = "dirscanner.";

    private static final String REPORT_PROPERTY_PREFIX = "report.";

    private static final String PARAMS_SUFIX = ".params";

    private static final String TYPES_SUFIX = ".types";


    public BooterConfiguration deserialize( InputStream inputStream )
        throws IOException
    {
        Properties properties = SystemPropertyManager.loadProperties( inputStream );
        final List reports = new ArrayList();
        Object[] dirScannerParams = null;
        boolean enableAssertions = false;
        boolean childDelegation = true;
        SuiteDefinition suiteDefinition = null;
        boolean failIfNotests = false;  // todo; check this out.
        boolean useSystemClassLoader = false; // todo check default value
        boolean useManifestOnlyJar = false; // todo check default value

        SortedMap classPathUrls = new TreeMap();

        SortedMap surefireClassPathUrls = new TreeMap();

        Collection booterClassPathUrl = new ArrayList();

        for ( Enumeration e = properties.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if ( name.startsWith( REPORT_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) &&
                !name.endsWith( TYPES_SUFIX ) )
            {
                String className = properties.getProperty( name );

                String params = properties.getProperty( name + PARAMS_SUFIX );
                String types = properties.getProperty( name + TYPES_SUFIX );
                reports.add( new Object[]{ className, constructParamObjects( params, types ) } );
            }
            else if ( name.startsWith( TEST_SUITE_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) &&
                !name.endsWith( TYPES_SUFIX ) )
            {
                String className = properties.getProperty( name );

                String params = properties.getProperty( name + PARAMS_SUFIX );
                String types = properties.getProperty( name + TYPES_SUFIX );
                suiteDefinition = new SuiteDefinition( className, constructParamObjects( params, types ) );
            }
            else if ( name.startsWith( DIRSCANNER_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) &&
                !name.endsWith( TYPES_SUFIX ) )
            {
                String params = properties.getProperty( name + PARAMS_SUFIX );
                String types = properties.getProperty( name + TYPES_SUFIX );
                dirScannerParams = constructParamObjects( params, types );
            }
            else if ( name.startsWith( "classPathUrl." ) )
            {
                classPathUrls.put( Integer.valueOf( name.substring( name.indexOf( '.' ) + 1 ) ),
                                   properties.getProperty( name ) );
            }
            else if ( name.startsWith( "surefireClassPathUrl." ) )
            {
                surefireClassPathUrls.put( Integer.valueOf( name.substring( name.indexOf( '.' ) + 1 ) ),
                                           properties.getProperty( name ) );
            }
            else if ( name.startsWith( "surefireBootClassPathUrl." ) )
            {
                booterClassPathUrl.add( properties.getProperty( name ) );
            }
            else if ( "childDelegation".equals( name ) )
            {
                childDelegation = Boolean.valueOf( properties.getProperty( "childDelegation" ) ).booleanValue();
            }
            else if ( "enableAssertions".equals( name ) )
            {
                enableAssertions = Boolean.valueOf( properties.getProperty( "enableAssertions" ) ).booleanValue();
            }
            else if ( "useSystemClassLoader".equals( name ) )
            {
                useSystemClassLoader =
                    Boolean.valueOf( properties.getProperty( "useSystemClassLoader" ) ).booleanValue();
            }
            else if ( "useManifestOnlyJar".equals( name ) )
            {
                useManifestOnlyJar = Boolean.valueOf( properties.getProperty( "useManifestOnlyJar" ) ).booleanValue();
            }
            else if ( "failIfNoTests".equals( name ) )
            {
                failIfNotests = Boolean.valueOf( properties.getProperty( "failIfNoTests" ) ).booleanValue();
            }
        }

        // todo check out this "never" value
        ClassLoaderConfiguration forkConfiguration =
            new ClassLoaderConfiguration( useSystemClassLoader, useManifestOnlyJar );

        ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( classPathUrls, surefireClassPathUrls, booterClassPathUrl, enableAssertions,
                                        childDelegation );

        return new BooterConfiguration( forkConfiguration, classpathConfiguration, suiteDefinition, reports,
                                        dirScannerParams, failIfNotests, properties );
    }

    void writePropertiesFile( File file, String name, Properties properties )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( file );

        try
        {
            properties.store( out, name );
        }
        finally
        {
            close( out );
        }
    }

    private static List processStringList( String stringList )
    {
        String sl = stringList;

        if ( sl.startsWith( "[" ) && sl.endsWith( "]" ) )
        {
            sl = sl.substring( 1, sl.length() - 1 );
        }

        List list = new ArrayList();

        String[] stringArray = StringUtils.split( sl, "," );

        for ( int i = 0; i < stringArray.length; i++ )
        {
            list.add( stringArray[i].trim() );
        }
        return list;
    }

    private static Object[] constructParamObjects( String paramProperty, String typeProperty )
    {
        Object[] paramObjects = null;
        if ( paramProperty != null )
        {
            // bit of a glitch that it need sto be done twice to do an odd number of vertical bars (eg |||, |||||).
            String[] params = StringUtils.split(
                StringUtils.replace( StringUtils.replace( paramProperty, "||", "| |" ), "||", "| |" ), "|" );
            String[] types =
                StringUtils.split( StringUtils.replace( StringUtils.replace( typeProperty, "||", "| |" ), "||", "| |" ),
                                   "|" );

            paramObjects = new Object[params.length];

            for ( int i = 0; i < types.length; i++ )
            {
                if ( types[i].trim().length() == 0 )
                {
                    params[i] = null;
                }
                else if ( types[i].equals( String.class.getName() ) )
                {
                    paramObjects[i] = params[i];
                }
                else if ( types[i].equals( File.class.getName() ) )
                {
                    paramObjects[i] = new File( params[i] );
                }
                else if ( types[i].equals( File[].class.getName() ) )
                {
                    List stringList = processStringList( params[i] );
                    File[] fileList = new File[stringList.size()];
                    for ( int j = 0; j < stringList.size(); j++ )
                    {
                        fileList[j] = new File( (String) stringList.get( j ) );
                    }
                    paramObjects[i] = fileList;
                }
                else if ( types[i].equals( ArrayList.class.getName() ) )
                {
                    paramObjects[i] = processStringList( params[i] );
                }
                else if ( types[i].equals( Boolean.class.getName() ) )
                {
                    paramObjects[i] = Boolean.valueOf( params[i] );
                }
                else if ( types[i].equals( Integer.class.getName() ) )
                {
                    paramObjects[i] = Integer.valueOf( params[i] );
                }
                else if ( types[i].equals( Properties.class.getName() ) )
                {
                    final Properties result = new Properties();
                    final String value = params[i];
                    try
                    {
                        ByteArrayInputStream bais = new ByteArrayInputStream( value.getBytes( "8859_1" ) );
                        result.load( bais );
                    }
                    catch ( Exception e )
                    {
                        throw new RuntimeException( "bug in property conversion", e );
                    }
                    paramObjects[i] = result;
                }
                else
                {
                    // TODO: could attempt to construct with a String constructor if needed
                    throw new IllegalArgumentException( "Unknown parameter type: " + types[i] );
                }
            }
        }
        return paramObjects;
    }

    /**
     * From IOUtils
     * Closes the output stream. The output stream can be null and any IOException's will be swallowed.
     *
     * @param outputStream The stream to close.
     */
    public static void close( OutputStream outputStream )
    {
        if ( outputStream == null )
        {
            return;
        }

        try
        {
            outputStream.close();
        }
        catch( IOException ex )
        {
            // ignore
        }
    }

}
