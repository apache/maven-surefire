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


/**
 * Test project using JUnit4.4 -dep.  junit-dep includes only junit.* classes, and depends explicitly on hamcrest-core
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class JUnit44DepIT
    extends SurefireVerifierTestClass
{
    public JUnit44DepIT()
    {
        super( "/junit44-dep" );
    }

    public void testJUnit44Dep()
        throws Exception
    {
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 1, 0, 0, 0 );
    }
    public void testJUnit44DepWithSneaky381()
        throws Exception
    {
        activateProfile("provided381");
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 1, 0, 0, 0 );
    }


}
