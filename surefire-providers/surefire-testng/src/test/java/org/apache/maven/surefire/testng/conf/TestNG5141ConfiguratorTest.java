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

import junit.framework.TestCase;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.maven.surefire.testng.conf.TestNGMapConfiguratorTest.*;

public class TestNG5141ConfiguratorTest
    extends TestCase
{

    public void testListenersOnSeparateLines()
            throws Exception
    {
        try
        {
            String listenersOnSeveralLines = String.format("%s , %n %s",
                                                           FIRST_LISTENER, SECOND_LISTENER);
            Map convertedOptions = getConvertedOptions(LISTENER_PROP, listenersOnSeveralLines);
            List listeners = (List) convertedOptions.get(String.format("-%s", LISTENER_PROP));
            assertEquals(2, listeners.size());
            fail();
        }
        catch ( TestSetFailedException e )
        {
            // TODO remove it when surefire will use "configure(CommandLineArgs)"
        }
    }

    public void testListenersOnTheSameLine()
            throws Exception
    {
        try {
            String listenersOnSeveralLines = String.format("%s,%s",
                                                           FIRST_LISTENER, SECOND_LISTENER);
            Map convertedOptions = getConvertedOptions(LISTENER_PROP, listenersOnSeveralLines);
            List listeners = (List) convertedOptions.get(String.format("-%s", LISTENER_PROP));
            assertEquals(2, listeners.size());
            fail();
        }
        catch ( TestSetFailedException e )
        {
            // TODO remove it when surefire will use "configure(CommandLineArgs)"
        }
    }

    private Map getConvertedOptions( String key, String value )
            throws TestSetFailedException
    {
        TestNGMapConfigurator testNGMapConfigurator = new TestNG5141Configurator();
        Map<String, String> raw = new HashMap<>();
        raw.put( key, value );
        return testNGMapConfigurator.getConvertedOptions( raw );
    }
}
