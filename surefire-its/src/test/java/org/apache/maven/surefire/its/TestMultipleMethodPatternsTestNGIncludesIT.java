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

import org.apache.maven.surefire.its.fixture.Settings;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.its.fixture.Configuration.TEST;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TestNG test project using multiple method patterns, including wildcards in class and method names.
 * Uses {@link Settings#TestNG_INCLUDES}.
 *
 * @see TestMultipleMethodPatternsTestNGIT
 * @see TestMultipleMethodPatternsTestNGIncludesExcludesIT
 */
public class TestMultipleMethodPatternsTestNGIncludesIT extends AbstractTestMultipleMethodPatterns {

    @Override
    protected Settings getSettings() {
        return Settings.TestNG_INCLUDES;
    }

    @Override
    protected SurefireLauncher unpack() {
        return unpack("testng-multiple-method-patterns", "_" + getSettings().path())
                .setForkJvm();
    }

    @Override
    @Test
    @Disabled("TestNG does not support regex method patterns")
    public void regexClassAndMethod() {
        super.regexClassAndMethod();
    }

    @Override
    @Test
    @Disabled("TestNG does not support regex method patterns")
    public void testRegexSuccessTwo() {
        assumeTrue(getSettings().getConfiguration() == TEST, "Configuration is TEST");
        super.regexClassAndMethod();
    }
}
