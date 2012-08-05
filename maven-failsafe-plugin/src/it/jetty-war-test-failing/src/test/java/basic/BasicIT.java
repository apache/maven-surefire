package basic;

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

import java.io.IOException;
import java.util.Properties;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import junit.framework.TestCase;

public class BasicIT
    extends TestCase
{

    private WebClient client;

    private String baseUrl;

    private final Properties properties = new Properties();

    public void setUp()
        throws IOException
    {
        client = new WebClient();
        properties.load( getClass().getResourceAsStream( "/integration-test.properties" ) );
        baseUrl = properties.getProperty( "baseUrl" );
    }

    public void tearDown()
    {
        if ( client != null )
        {
            client.closeAllWindows();
            client = null;
        }
    }

    public void testSmokes()
        throws Exception
    {
        client.setThrowExceptionOnFailingStatusCode( false );
        HtmlPage page = client.getPage( baseUrl + "index.html" );
        assertFalse( page.asText().contains( "Hello World" ) );
    }
}
