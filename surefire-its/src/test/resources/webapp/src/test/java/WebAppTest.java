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

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

import org.mortbay.jetty.Server;


public class WebAppTest
    extends TestCase
{
    private Server server = null;

    public void setUp()
        throws Exception
    {
        System.setProperty( "org.mortbay.xml.XmlParser.NotValidating", "true" );

        server = new Server();
        String testPort = ":18080";
        server.addListener( testPort );
        server.addWebApplication( "127.0.0.1", "/webapp", "target/webapp" );

        server.start();
    }

    public void testBlah()
        throws Exception
    {
        URL url = new URL( "http://127.0.0.1:18080/webapp/index.jsp" );
        InputStream stream = url.openStream();
        StringBuffer sb = new StringBuffer();
        for ( int i = stream.read(); i != -1; i = stream.read() )
        {
            sb.append( (char) i );
        }
        String value = sb.toString();
        assertTrue( value, value.contains( "Hello" ) );
    }

    public void tearDown()
        throws Exception
    {
        if ( server != null )
            server.stop();
    }
}
