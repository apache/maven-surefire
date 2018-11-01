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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kristian Rosenvold
 */
public class SystemPropertyManager
{

    /**
     * Loads the properties, closes the stream
     *
     * @param inStream The stream to read from, will be closed
     * @return The properties
     * @throws java.io.IOException If something bad happens
     */
    public static PropertiesWrapper loadProperties( InputStream inStream )
        throws IOException
    {
        try ( final InputStream stream = inStream )
        {
            Properties p = new Properties();
            p.load( stream );
            Map<String, String> map = new ConcurrentHashMap<>( p.size() );
            for ( String key : p.stringPropertyNames() )
            {
                map.put( key, p.getProperty( key ) );
            }
            return new PropertiesWrapper( map );
        }
    }

    private static PropertiesWrapper loadProperties( File file )
        throws IOException
    {
        return loadProperties( new FileInputStream( file ) );
    }

    public static void setSystemProperties( File file )
        throws IOException
    {
        PropertiesWrapper p = loadProperties( file );
        p.setAsSystemProperties();
    }

    public static File writePropertiesFile( Properties properties, File tempDirectory, String name,
                                            boolean keepForkFiles )
        throws IOException
    {
        File file = File.createTempFile( name, "tmp", tempDirectory );
        if ( !keepForkFiles )
        {
            file.deleteOnExit();
        }

        writePropertiesFile( file, name, properties );

        return file;
    }

    public static void writePropertiesFile( File file, String name, Properties properties )
        throws IOException
    {
        try ( FileOutputStream out = new FileOutputStream( file ) )
        {
            properties.store( out, name );
        }
    }
}
