package systemProperties;

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

import junit.framework.TestCase;

public class BasicTest
    extends TestCase
{


    public void testSetInPom()
    {
        assertEquals( "property setInPom not set", "foo", System.getProperty( "setInPom" ) );
    }

    public void testSetOnArgLine()
    {
        assertEquals( "setOnArgLine property not set", "bar", System.getProperty( "setOnArgLine" ) );
    }


    public void testSystemPropertyUsingMavenProjectProperties()
    {
        String actualBuildDirectory = new File( System.getProperty( "basedir" ), "target" ).getAbsolutePath();

        String buildDirectoryFromPom = new File( System.getProperty( "buildDirectory" ) ).getAbsolutePath();

        assertEquals( "Pom property not set.", actualBuildDirectory, buildDirectoryFromPom );
    }

    public void testSystemPropertyGenerateByOtherPlugin()
        throws Exception
    {
        int reservedPort1 = Integer.parseInt( System.getProperty( "reservedPort1" ) );
        int reservedPort2 = Integer.parseInt( System.getProperty( "reservedPort2" ) );
        System.out.println( "reservedPort1: " + reservedPort1 );
        System.out.println( "reservedPort2: " + reservedPort2 );

        assertTrue( reservedPort1 != reservedPort2 );
    }

    public void testEmptySystemProperties()
    {
        assertNull( "Null property is not null", System.getProperty( "nullProperty" ) );
        assertEquals( "Empty property is not empty", "", System.getProperty( "emptyProperty" ) );
        assertNotNull( "Blank property is null", System.getProperty( "blankProperty" ) );
        assertEquals( "Blank property is not trimmed", "", System.getProperty( "blankProperty" ) );
    }

    /**
     * work around for SUREFIRE-121
     */
    public void testSetOnArgLineWorkAround()
    {
        assertEquals( "property setOnArgLineWorkAround not set", "baz",
                      System.getProperty( "setOnArgLineWorkAround" ) );
    }

    public void testSetOnMavenCommandLine()
    {
        assertEquals( "property setOnMavenCommandLine not set", "baz", System.getProperty( "setOnMavenCommandLine" ) );
    }

    public void testSetInFile()
    {
        assertEquals( "property setInFile not set", "bar", System.getProperty( "setInFile" ) );
        assertEquals( "property overriddenPropertyFomFile not overridden", "value2",
                      System.getProperty( "overriddenPropertyFomFile" ) );
    }
}
