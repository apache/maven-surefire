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
package org.apache.maven.plugin.surefire.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.util.ScanResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kristian Rosenvold
 */
public class DirectoryScannerTest {
    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("**/*ZT*A.java", 3, true),
                Arguments.of("**/*ZT*A.java#testMethod", 3, true),
                Arguments.of("**/*ZT?A.java#testMethod, !*ZT2A", 2, true),
                Arguments.of("**/*ZT?A.java#testMethod, !*ZT2A#testMethod", 3, true),
                Arguments.of("#testMethod", 3, false));
    }

    @ParameterizedTest(name = "\"{0}\" should count {1} classes")
    @MethodSource("data")
    public void locateTestClasses(String filter, int expectedClassesCount, boolean exact) throws Exception {
        // use target as people can configure ide to compile in an other place than maven
        File baseDir = new File(new File("target/test-classes").getCanonicalPath());
        TestListResolver resolver = new TestListResolver(filter);
        DirectoryScanner surefireDirectoryScanner = new DirectoryScanner(baseDir, resolver);

        ScanResult classNames = surefireDirectoryScanner.scan();
        assertThat(classNames).isNotNull();
        if (exact) {
            assertThat(classNames.size()).isEqualTo(expectedClassesCount);
        } else {
            assertThat(classNames.size()).isGreaterThanOrEqualTo(expectedClassesCount);
        }

        Map<String, String> props = new HashMap<>();
        classNames.writeTo(props);
        if (exact) {
            assertThat(props.values()).hasSize(expectedClassesCount);
        } else {
            assertThat(props.values().size()).isGreaterThanOrEqualTo(expectedClassesCount);
        }
    }
}
