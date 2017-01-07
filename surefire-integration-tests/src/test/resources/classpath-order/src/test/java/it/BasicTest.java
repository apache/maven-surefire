package it;

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

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BasicTest
        extends TestCase
{

    public void testTestClassesBeforeMainClasses()
            throws IOException
    {
        Properties props = getProperties( "/surefire-classpath-order.properties" );
        assertEquals( "test-classes", props.getProperty( "Surefire" ) );
    }

    public void testMainClassesBeforeDependencies()
            throws IOException
    {
        Properties props = getProperties( "/surefire-report.properties" );
        assertEquals( "classes", props.getProperty( "Surefire" ) );
    }

    private Properties getProperties( String resource )
        throws IOException
    {
        InputStream in = null;
        try
        {
            in = getClass().getResourceAsStream( resource );
            assertNotNull( in );
            Properties props = new Properties();
            props.load( in );
            in.close();
            in = null;
            return props;
        }
        catch ( IOException e )
        {
            fail( e.toString() );
            return null;
        }
        finally
        {
            try
            {
                if ( in != null )
                {
                    in.close();
                }
            }
            catch ( final IOException e )
            {
                // Suppressed.
            }
        }
    }

}
