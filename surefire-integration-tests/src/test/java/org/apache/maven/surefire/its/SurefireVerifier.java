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
import java.util.List;
import java.util.Map;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.surefire.its.misc.HelperAssertions;

/**
 * A specialized verifier that enforces a standard use case for surefire IT's
 *
 * @author Kristian Rosenvold
 */
public class SurefireVerifier
{
    private final Verifier verifier;
    
    private final File baseDir;

    public static SurefireVerifier fromDirectory(File testDir)
    {
        try
        {
            return new SurefireVerifier( new Verifier( testDir.getAbsolutePath()) , testDir);
        }
        catch ( VerificationException e )
        {
            throw new RuntimeException( e );
        }

    }

    public SurefireVerifier( Verifier verifier, File baseDir )
    {
        this.verifier = verifier;
        this.baseDir = baseDir;
    }

    public void assertFilePresent( String file )
    {
        verifier.assertFilePresent( file );
    }

    public void assertFileNotPresent( String file )
    {
        verifier.assertFileNotPresent( file );
    }

    public void executeGoals( List goals, Map envVars )
        throws VerificationException
    {
        verifier.executeGoals( goals, envVars );
    }

    public void setCliOptions( List cliOptions )
    {
        verifier.setCliOptions( cliOptions );
    }

    public void resetStreams()
    {
        verifier.resetStreams();
    }

    public void verifyTextInLog( String text )
        throws VerificationException
    {
        verifier.verifyTextInLog( text );
    }


    public void verifyErrorFreeLog()
        throws VerificationException
    {
        verifier.verifyErrorFreeLog();
    }

    public List loadFile( String basedir, String filename, boolean hasCommand )
        throws VerificationException
    {
        return verifier.loadFile( basedir, filename, hasCommand );
    }

    public List loadFile( File file, boolean hasCommand )
        throws VerificationException
    {
        return verifier.loadFile( file, hasCommand );
    }

    public String getLogFileName()
    {
        return verifier.getLogFileName();
    }


    public String getBasedir()
    {
        return verifier.getBasedir();
    }

    public String getMavenVersion()
        throws VerificationException
    {
        return verifier.getMavenVersion();
    }


    public String getArtifactPath( String org, String name, String version, String ext )
    {
        return verifier.getArtifactPath( org, name, version, ext );
    }

    
    public void assertTestSuiteResults( int total, int errors, int failures, int skipped )
    {
        HelperAssertions.assertTestSuiteResults( total, errors, failures, skipped, baseDir );
    }

    
    
    
}
