/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.testng.conf;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author <a href='mailto:marvin[at]marvinformatics[dot]com'>Marvin Froeder</a>
 */
public class TestNG60ConfiguratorTest extends TestCase {

    public void testGetConvertedOptions() throws Exception {
        TestNGMapConfigurator testNGMapConfigurator = new TestNG60Configurator();
        Map raw = new HashMap();
        raw.put("objectfactory", "java.lang.String");
        Map convertedOptions = testNGMapConfigurator.getConvertedOptions(raw);
        String objectfactory = (String) convertedOptions.get("-objectfactory");
        assertNotNull(objectfactory);
    }
}
