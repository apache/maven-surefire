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
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.testset.TestSetFailedException;

import junit.framework.TestCase;
import org.testng.ReporterConfig;

/**
 * @author Kristian Rosenvold
 */
public class TestNGMapConfiguratorTest
    extends TestCase
{
    public static final String FIRST_LISTENER = "org.testng.TestListenerAdapter";
    public static final String SECOND_LISTENER = "org.testng.reporters.ExitCodeListener";
    public static final String LISTENER_PROP = "listener";

    public void testGetConvertedOptions()
        throws Exception
    {
        Map convertedOptions = getConvertedOptions( "mixed", "true" );
        boolean bool = (Boolean) convertedOptions.get( "-mixed" );
        assertTrue( bool );
    }

    public void testListenersOnSeparateLines()
        throws Exception
    {
        String listenersOnSeveralLines = String.format( "%s , %n %s",
                FIRST_LISTENER, SECOND_LISTENER);
        Map convertedOptions = getConvertedOptions(LISTENER_PROP, listenersOnSeveralLines);
        List listeners = (List) convertedOptions.get( String.format("-%s", LISTENER_PROP));
        assertEquals(2, listeners.size());
    }

    public void testListenersOnTheSameLine()
        throws Exception
    {
        String listenersOnSeveralLines = String.format( "%s,%s",
                FIRST_LISTENER, SECOND_LISTENER);
        Map convertedOptions = getConvertedOptions( LISTENER_PROP, listenersOnSeveralLines);
        List listeners = (List) convertedOptions.get( String.format("-%s", LISTENER_PROP));
        assertEquals(2, listeners.size());
    }

    public void testGroupByInstances()
        throws Exception
    {
        Map convertedOptions = getConvertedOptions( "group-by-instances", "true" );
        boolean bool = (Boolean) convertedOptions.get( "-group-by-instances" );
        assertTrue( bool );
    }

    public void testReporter()
        throws Exception
    {
        Map<String, Object> convertedOptions = getConvertedOptions( "reporter", "classname" );
        List<ReporterConfig> reporter = (List) convertedOptions.get( "-reporterslist" );
        ReporterConfig reporterConfig = reporter.get( 0 );
        assertEquals( "classname", reporterConfig.getClassName() );
    }

    private Map getConvertedOptions( String key, String value )
        throws TestSetFailedException
    {
        TestNGMapConfigurator testNGMapConfigurator = new TestNGMapConfigurator();
        Map<String, String> raw = new HashMap<>();
        raw.put( key, value );
        return testNGMapConfigurator.getConvertedOptions( raw );
    }
}
