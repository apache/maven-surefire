package org.apache.maven.surefire.its.fixture;

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

import static java.lang.Double.parseDouble;

/**
 * Contains commonly used features for most tests, encapsulating
 * common use cases.
 * <br>
 * Also includes thread-safe access to the extracted resource
 * files, which AbstractSurefireIntegrationTestClass does not.
 * Thread safe only for running in "classes" mode.
 *
 * @author Kristian Rosenvold
 */
public abstract class SurefireJUnit4IntegrationTestCase
{
    private static final int JAVA9_VERSION = 9;

    public static final double JAVA_VERSION = javaVersion();

    public static final boolean IS_JAVA9_PLUS = isJDK9Plus();

    public OutputValidator executeErrorFreeTest( String sourceName, int total )
    {
        return unpack( sourceName ).executeTest().verifyErrorFree( total );
    }

    public SurefireLauncher unpack( String sourceName )
    {
        return unpack( getClass(), sourceName, "" );
    }

    public SurefireLauncher unpack( String sourceName, String suffix )
    {
        return unpack( getClass(), sourceName, suffix );
    }

    public SurefireLauncher unpack( String sourceName, String suffix, String[] cli )
    {
        return unpack( getClass(), sourceName, suffix, cli );
    }

    public static SurefireLauncher unpack( Class<?> testClass, String sourceName, String suffix )
    {
        return unpack( testClass, sourceName, suffix, null );
    }

    private static SurefireLauncher unpack( Class<?> testClass, String sourceName, String suffix, String[] cli )
    {
        MavenLauncher mavenLauncher = new MavenLauncher( testClass, sourceName, suffix, cli );
        return new SurefireLauncher( mavenLauncher );
    }

    private static double javaVersion()
    {
        return parseDouble( System.getProperty( "java.specification.version" ) );
    }

    private static boolean isJDK9Plus()
    {
        return javaVersion() >= JAVA9_VERSION;
    }
}
