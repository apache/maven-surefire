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

import java.io.IOException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.SurefireVerifierTestClass2;

/**
 * Test failIfNoTests with various forkModes.
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class CheckTestFailIfNoTestsForkModeIT
    extends SurefireVerifierTestClass2
{
    public void testFailIfNoTestsForkModeAlways()
        throws Exception
    {
        doTest( unpack().forkAlways().failIfNoTests( true ) );
    }


    public void testFailIfNoTestsForkModeNever()
        throws Exception
    {
        doTest( unpack().forkNever().failIfNoTests( true ) );
    }

    public void testFailIfNoTestsForkModeOnce()
        throws Exception
    {
        doTest( unpack().forkOnce().failIfNoTests( true ) );
    }

    public void testDontFailIfNoTestsForkModeAlways()
        throws Exception
    {
        doTest( unpack().forkAlways().failIfNoTests( false) );
    }

    public void testDontFailIfNoTestsForkModeNever()
        throws Exception
    {
        doTest( unpack().forkNever().failIfNoTests( false) );
    }

    public void testDontFailIfNoTestsForkModeOnce()
        throws Exception
    {
        doTest( unpack().forkOnce().failIfNoTests( false) );
    }

    private void doTest( SurefireLauncher launcher )
        throws IOException, VerificationException
    {
        if ( launcher.isFailIfNoTests() )
        {
             launcher.executeTestWithFailure();
        }
        else
        {
            launcher.executeTest().verifyErrorFreeLog().assertTestSuiteResults( 0, 0, 0, 0 );
        }
    }

    private SurefireLauncher unpack()
        throws VerificationException, IOException
    {
        return unpack("default-configuration-classWithNoTests");
    }

}
