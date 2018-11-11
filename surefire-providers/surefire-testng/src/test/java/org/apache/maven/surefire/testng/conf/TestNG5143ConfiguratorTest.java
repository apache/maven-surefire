package org.apache.maven.surefire.testng.conf;

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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.maven.surefire.testset.TestSetFailedException;

import static org.apache.maven.surefire.testng.conf.TestNGMapConfiguratorTest.FIRST_LISTENER;
import static org.apache.maven.surefire.testng.conf.TestNGMapConfiguratorTest.LISTENER_PROP;
import static org.apache.maven.surefire.testng.conf.TestNGMapConfiguratorTest.SECOND_LISTENER;

public class TestNG5143ConfiguratorTest
    extends TestCase
{
    public void testListenersOnSeparateLines()
            throws Exception
    {
        String listenersOnSeveralLines = String.format( "%s , %n %s",
                FIRST_LISTENER, SECOND_LISTENER);
        Map convertedOptions = getConvertedOptions(LISTENER_PROP, listenersOnSeveralLines);
        String listeners = (String) convertedOptions.get( String.format("-%s", LISTENER_PROP));
        assertEquals(FIRST_LISTENER + "," + SECOND_LISTENER, listeners);
    }

    public void testListenersOnTheSameLine()
            throws Exception
    {
        String listenersOnSeveralLines = String.format( "%s,%s",
                FIRST_LISTENER, SECOND_LISTENER);
        Map convertedOptions = getConvertedOptions( LISTENER_PROP, listenersOnSeveralLines);
        String listeners = (String) convertedOptions.get( String.format("-%s", LISTENER_PROP));
        assertEquals(FIRST_LISTENER + "," + SECOND_LISTENER, listeners);
    }

    public void testReporter()
            throws Exception
    {
        Map<String, Object> convertedOptions = getConvertedOptions( "reporter", "classname" );
        assertNull( "classname", convertedOptions.get( "-reporterslist" ) );
        String reporter = (String) convertedOptions.get("-reporter" );
        assertEquals( "classname", reporter );
    }

    private Map getConvertedOptions( String key, String value )
            throws TestSetFailedException
    {
        TestNGMapConfigurator testNGMapConfigurator = new TestNG5143Configurator();
        Map<String, String> raw = new HashMap<>();
        raw.put( key, value );
        return testNGMapConfigurator.getConvertedOptions( raw );
    }
}
