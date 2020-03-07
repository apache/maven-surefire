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

import org.apache.maven.surefire.its.fixture.SurefireLauncher;

/**
 * Enum listing all the JUnit version.
 */
public enum JUnitVersion
{

    JUNIT_4_0( "4.0" ),
    JUNIT_4_1( "4.1" ),
    JUNIT_4_2( "4.2" ),
    JUNIT_4_3( "4.3" ),
    JUNIT_4_3_1( "4.3.1" ),
    JUNIT_4_4( "4.4" ),
    JUNIT_4_5( "4.5" ),
    JUNIT_4_6( "4.6" ),
    JUNIT_4_7( "4.7" ),
    JUNIT_4_8( "4.8" ),
    JUNIT_4_8_1( "4.8.1" ),
    JUNIT_4_8_2( "4.8.2" ),
    JUNIT_4_9( "4.9" ),
    JUNIT_4_10( "4.10" ),
    JUNIT_4_11( "4.11" ),
    JUNIT_4_12( "4.12" ),
    JUNIT_4_13( "4.13" );

    private final String version;

    JUnitVersion( String version )
    {
        this.version = version;
    }

    public SurefireLauncher configure( SurefireLauncher launcher )
    {
        return launcher.setJUnitVersion( version );
    }

    public String toString()
    {
        return version;
    }
}
