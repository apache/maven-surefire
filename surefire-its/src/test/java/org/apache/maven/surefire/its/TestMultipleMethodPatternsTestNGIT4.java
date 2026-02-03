/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.its;

import java.util.Arrays;

import org.apache.maven.surefire.its.fixture.Settings;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.apache.maven.surefire.its.fixture.Configuration.TEST;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

/**
 * TestNG test project using multiple method patterns, including wildcards in class and method names.
 */
@RunWith(Parameterized.class)
public class TestMultipleMethodPatternsTestNGIT4 extends AbstractTestMultipleMethodPatterns4 {
    private final Settings settings;

    public TestMultipleMethodPatternsTestNGIT4(Settings settings) {
        this.settings = settings;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][] {{Settings.TestNG_TEST}, {Settings.TestNG_INCLUDES}, {Settings.TestNG_INCLUDES_EXCLUDES}
                });
    }

    @Override
    protected Settings getSettings() {
        return settings;
    }

    @Override
    protected SurefireLauncher unpack() {
        return unpack("testng-multiple-method-patterns", "_" + settings.path());
    }

    @Override
    @Test
    @Ignore("TestNG does not support regex method patterns")
    public void regexClassAndMethod() {
        super.regexClassAndMethod();
    }

    @Override
    @Test
    @Ignore("TestNG does not support regex method patterns")
    public void testRegexSuccessTwo() {
        assumeThat(getSettings().getConfiguration(), is(TEST));
        super.regexClassAndMethod();
    }
}
