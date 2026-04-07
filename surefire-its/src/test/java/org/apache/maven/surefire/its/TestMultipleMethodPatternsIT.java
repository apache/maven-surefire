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

/**
 * JUnit test project using multiple method patterns, including wildcards in class and method names.
 * Uses {@link Settings#JUNIT4_INCLUDES}.
 *
 * @see TestMultipleMethodPatternsIncludesExcludesIT
 */
public class TestMultipleMethodPatternsIT extends AbstractTestMultipleMethodPatterns {

    @Override
    protected Settings getSettings() {
        return Settings.JUNIT4_INCLUDES;
    }

    @Override
    protected SurefireLauncher unpack() {
        return unpack("junit4-multiple-method-patterns", "_" + getSettings().path())
                .setForkJvm();
    }
}
