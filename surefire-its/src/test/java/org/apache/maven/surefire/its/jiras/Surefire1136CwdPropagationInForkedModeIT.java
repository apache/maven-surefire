package org.apache.maven.surefire.its.jiras;

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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * SUREFIRE-1136 Correct current working directory propagation in forked mode
 *
 * Note: variables expansion behaves differently on MVN 2.x since not existing variables
 * are resolved to 'null' value so that ${surefire.forkNumber} cannot work.
 *
 * @author Norbert Wnuk
 */
public class Surefire1136CwdPropagationInForkedModeIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testTestNgAndJUnitTogether()
    {
        OutputValidator outputValidator = unpack( "surefire-1136-cwd-propagation-in-forked-mode" ).executeTest();
        outputValidator.assertTestSuiteResults( 1, 0, 0, 0 );
    }
}
