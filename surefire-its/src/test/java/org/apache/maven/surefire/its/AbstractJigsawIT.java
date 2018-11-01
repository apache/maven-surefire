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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;

import static org.apache.maven.surefire.its.fixture.SurefireLauncher.EXT_JDK_HOME;
import static org.apache.maven.surefire.its.fixture.SurefireLauncher.EXT_JDK_HOME_KEY;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Abstract test class for Jigsaw tests.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public abstract class AbstractJigsawIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final double JIGSAW_JAVA_VERSION = 9.0d;

    protected abstract String getProjectDirectoryName();

    protected SurefireLauncher assumeJigsaw() throws IOException
    {
        assumeTrue( "There's no JDK 9 provided.",
                          isJavaVersion9AtLeast() || EXT_JDK_HOME != null && isExtJavaVerion9AtLeast() );
        // fail( EXT_JDK_HOME_KEY + " was provided with value " + EXT_JDK_HOME + " but it is not Jigsaw Java 9." );

        SurefireLauncher launcher = unpack();

        return EXT_JDK_HOME == null ? launcher : launcher.setLauncherJavaHome( EXT_JDK_HOME );
    }

    protected SurefireLauncher assumeJava9Property() throws IOException
    {
        assumeTrue( "There's no JDK 9 provided.", EXT_JDK_HOME != null && isExtJavaVerion9AtLeast() );
        return unpack();
    }

    private SurefireLauncher unpack()
    {
        return unpack( getProjectDirectoryName() );
    }

    private static boolean isJavaVersion9AtLeast()
    {
        return Double.valueOf( System.getProperty( "java.specification.version" ) ) >= JIGSAW_JAVA_VERSION;
    }

    private static boolean isExtJavaVerion9AtLeast() throws IOException
    {
        File release = new File( EXT_JDK_HOME, "release" );
        assumeTrue( EXT_JDK_HOME_KEY + " was provided with value " + EXT_JDK_HOME + " but file does not exist "
                + EXT_JDK_HOME + File.separator + "release", release.exists() );

        Properties properties = new Properties();
        try ( InputStream is = new FileInputStream( release ) )
        {
            properties.load( is );
        }
        String javaVersion = properties.getProperty( "JAVA_VERSION" ).replace( "\"", "" );
        StringTokenizer versions = new StringTokenizer( javaVersion, "._" );

        if ( versions.countTokens() == 1 )
        {
            javaVersion = versions.nextToken();
        }
        else if ( versions.countTokens() >= 2 )
        {
            javaVersion = versions.nextToken() + "." + versions.nextToken();
        }
        else
        {
            fail( "unexpected java version format" );
        }

        return Double.valueOf( javaVersion ) >= JIGSAW_JAVA_VERSION;
    }
}
