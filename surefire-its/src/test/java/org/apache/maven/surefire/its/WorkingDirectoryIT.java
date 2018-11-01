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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test working directory configuration, SUREFIRE-416
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class WorkingDirectoryIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void testWorkingDirectory()
        throws Exception
    {
        final SurefireLauncher unpack = getUnpacked();
        final OutputValidator child = getPreparedChild( unpack );
        child.getTargetFile( "out.txt" ).delete();
        unpack.executeTest().verifyErrorFreeLog();
        child.assertTestSuiteResults( 1, 0, 0, 0 );
        verifyOutputDirectory( child );
    }

    @Test
    public void testWorkingDirectoryNoFork()
        throws Exception
    {
        final SurefireLauncher unpack = getUnpacked();
        final OutputValidator child = getPreparedChild( unpack );
        child.getTargetFile( "out.txt" ).delete();
        unpack.forkNever().executeTest().verifyErrorFreeLog();
        child.assertTestSuiteResults( 1, 0, 0, 0 );
        verifyOutputDirectory( child );
    }

    @Test
    public void testWorkingDirectoryChildOnly()
        throws Exception
    {
        final SurefireLauncher unpack = getUnpacked();
        final SurefireLauncher child = unpack.getSubProjectLauncher( "child" );
        child.getSubProjectValidator( "child" ).getTargetFile( "out.txt" ).delete();
        final OutputValidator outputValidator = child.executeTest().assertTestSuiteResults( 1, 0, 0, 0 );
        verifyOutputDirectory( outputValidator );
    }

    @Test
    public void testWorkingDirectoryChildOnlyNoFork()
        throws Exception
    {
        final SurefireLauncher unpack = getUnpacked();
        final SurefireLauncher child = unpack.getSubProjectLauncher( "child" );
        child.getSubProjectValidator( "child" ).getTargetFile( "out.txt" ).delete();
        final OutputValidator outputValidator = child.forkNever().executeTest().assertTestSuiteResults( 1, 0, 0, 0 );
        verifyOutputDirectory( outputValidator );
    }

    private SurefireLauncher getUnpacked()
    {
        return unpack( "working-directory" );
    }

    private OutputValidator getPreparedChild( SurefireLauncher unpack )
        throws VerificationException
    {
        final OutputValidator child = unpack.getSubProjectValidator( "child" );
        getOutFile( child ).delete();
        return child;
    }


    private TestFile getOutFile( OutputValidator child )
    {
        return child.getTargetFile( "out.txt" );
    }

    private void verifyOutputDirectory( OutputValidator childTestDir )
        throws IOException
    {
        final TestFile outFile = getOutFile( childTestDir );
        assertTrue( "out.txt doesn't exist: " + outFile.getAbsolutePath(), outFile.exists() );
        Properties p = new Properties();
        try ( FileInputStream is = outFile.getFileInputStream() )
        {
            p.load( is );
        }
        String userDirPath = p.getProperty( "user.dir" );
        assertNotNull( "user.dir was null in property file", userDirPath );
        File userDir = new File( userDirPath );
        // test if not a symlink
        if ( childTestDir.getBaseDir().getCanonicalFile().equals( childTestDir.getBaseDir().getAbsoluteFile() ) )
        {
            assertTrue( "wrong user.dir ! symlink ",
                        childTestDir.getBaseDir().getAbsolutePath().equalsIgnoreCase( userDir.getAbsolutePath() ) );
        }
        else
        {
            assertEquals( "wrong user.dir symlink ", childTestDir.getBaseDir().getCanonicalPath(),
                          userDir.getCanonicalPath() );
        }
    }
}
