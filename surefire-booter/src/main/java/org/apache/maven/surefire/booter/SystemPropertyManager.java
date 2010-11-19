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

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 */
public class SystemPropertyManager
{
    private static Properties loadProperties( File file )
        throws IOException
    {
        return loadProperties( new FileInputStream( file ) );
    }

    /*
    Loads the properties, closes the stream
     */

    public static Properties loadProperties( InputStream inStream )
        throws IOException
    {
        Properties p = new Properties();

        try
        {
            p.load( inStream );
        }
        finally
        {
            IOUtil.close( inStream );
        }

        return p;
    }


    public static void setSystemProperties( File file )
        throws IOException
    {
        Properties p = loadProperties( file );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, p.getProperty( key ) );
        }
    }
}
