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

import java.util.Arrays;

import org.apache.maven.surefire.its.fixture.Settings;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * TestNG test project using multiple method patterns, including wildcards in class and method names.
 */
@RunWith( Parameterized.class )
public class TestMultipleMethodPatternsTestNGIT
    extends AbstractTestMultipleMethodPatterns
{
    private final Settings settings;

    public TestMultipleMethodPatternsTestNGIT( Settings settings )
    {
        this.settings = settings;
    }

    @Parameters
    public static Iterable<Object[]> data()
    {
        return Arrays.asList( new Object[][]{
            { Settings.TestNG_TEST },
            { Settings.TestNG_INCLUDES },
            { Settings.TestNG_INCLUDES_EXCLUDES }
        } );
    }

    @Override
    protected Settings getSettings()
    {
        return settings;
    }

    @Override
    protected SurefireLauncher unpack()
    {
        return unpack( "testng-multiple-method-patterns", "_" + settings.path() );
    }
}
