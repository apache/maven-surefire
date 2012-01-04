package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.SurefireVerifierTestClass;

/**
 * SUREFIRE-697 Asserts proper truncation of long exception messages Some say testing this is a bit over the top.
 * 
 * @author Kristian Rosenvold
 */
public class Surefire697NiceSummaryIT
    extends SurefireVerifierTestClass
{
    public Surefire697NiceSummaryIT()
    {
        super( "/surefire-697-niceSummary" );
    }

    public void testBuildFailingWhenErrors()
        throws Exception
    {
        failNever();
        executeTest();
        verifyTextInLog( "testShortMultiline(junit.surefire697.BasicTest): A very short multiline message" );
        // Could assert that "Here is line 2" is not present too.
    }
}
