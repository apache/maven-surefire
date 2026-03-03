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
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES_EXCLUDES;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES_EXCLUDES_FILE;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES_FILE;
import static org.apache.maven.surefire.its.fixture.Configuration.TEST;
import static org.apache.maven.surefire.its.fixture.TestFramework.JUNIT4;
import static org.apache.maven.surefire.its.fixture.TestFramework.TestNG;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test project using multiple method patterns, including wildcards in class and method names.
 */
public abstract class AbstractTestMultipleMethodPatterns extends SurefireJUnit4IntegrationTestCase {
    private static final String CSV_DELIMITER_SHORT = ",";
    private static final String CSV_DELIMITER_LONG = ", ";
    private static final String NOT_DELIMITER = "!";

    protected abstract Settings getSettings();

    protected abstract SurefireLauncher unpack();

    protected SurefireLauncher prepare(String tests) {
        SurefireLauncher launcher = unpack().addGoal("-P " + getSettings().profile());
        String[] includedExcluded = splitIncludesExcludes(tests);
        switch (getSettings().getConfiguration()) {
            case TEST:
                launcher.setTestToRun(tests);
                break;
            case INCLUDES:
                launcher.sysProp("included", tests);
                break;
            case INCLUDES_EXCLUDES:
                launcher.sysProp("included", includedExcluded[0]);
                launcher.sysProp("excluded", includedExcluded[1]);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported configuration " + getSettings().getConfiguration());
        }
        return launcher;
    }

    private static String[] splitIncludesExcludes(String patterns) {
        String included = "";
        String excluded = "";
        for (String pattern : patterns.split(CSV_DELIMITER_SHORT)) {
            pattern = pattern.trim();
            if (pattern.startsWith(NOT_DELIMITER)) {
                excluded += pattern.substring(NOT_DELIMITER.length()).trim();
                excluded += CSV_DELIMITER_LONG;
            } else {
                included += pattern;
                included += CSV_DELIMITER_LONG;
            }
        }
        return new String[] {trimEndComma(included), trimEndComma(excluded)};
    }

    private static String trimEndComma(String pattern) {
        pattern = pattern.trim();
        return pattern.endsWith(CSV_DELIMITER_LONG)
                ? pattern.substring(0, pattern.length() - CSV_DELIMITER_LONG.length())
                : pattern;
    }

    @Test
    public void simpleNameTest() {
        prepare("TestTwo")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void simpleNameTestAsParallel() {
        assumeTrue(getSettings().getFramework() == JUNIT4 || getSettings().getFramework() == TestNG);
        prepare("TestTwo")
                .parallel("classes")
                .useUnlimitedThreads()
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void simpleNameTestWithJavaExt() {
        prepare("TestTwo.java")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void simpleNameTestWithWildcardPkg() {
        prepare("**/TestTwo")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void simpleNameTestWithJavaExtWildcardPkg() {
        prepare("**/TestTwo.java")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void fullyQualifiedTest() {
        prepare("jiras/surefire745/TestTwo.java")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void shouldMatchSimpleClassNameAndMethod() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("BasicTest#testSuccessTwo")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    /**
     * This method name was shorten because it cause 261 character long path on Windows with Jenkins Pipeline.
     */
    @Test
    public void matchSimpleClassAndMethodWithJavaExt() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("BasicTest.java#testSuccessTwo")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    /**
     * This method name was shorten because it cause 261 character long path on Windows with Jenkins Pipeline.
     */
    @Test
    public void matchSimpleClassAndMethodWithWildcardPkg() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("**/BasicTest#testSuccessTwo")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    /**
     * This method name was shorten because it cause 261 character long path on Windows with Jenkins Pipeline.
     */
    @Test
    public void matchSimpleClassAndMethodWithJavaExtWildcardPkg() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("**/BasicTest.java#testSuccessTwo")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    @Test
    public void shouldMatchWildcardPackageAndClassAndMethod() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("jiras/**/BasicTest#testSuccessTwo")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    @Test
    public void regexClass() {
        prepare("%regex[.*.TestTwo.*]")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestTwo#testSuccessTwo");
    }

    @Test
    public void testSuccessTwo() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("#testSuccessTwo")
                .maven()
                .debugLogging()
                .executeTest()
                .verifyErrorFree(5)
                .verifyErrorFreeLog();
    }

    @Test
    public void testRegexSuccessTwo() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("%regex[#testSuccessTwo]").executeTest().verifyErrorFree(5).verifyErrorFreeLog();
    }

    @Test
    public void regexClassAndMethod() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("%regex[.*.BasicTest.*#testSuccessTwo]")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    @Test
    public void shouldMatchExactClassAndMethodWildcard() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("BasicTest#test*One")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessOne");
    }

    @Test
    public void shouldMatchExactClassAndMethodsWildcard() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("BasicTest#testSuccess*")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessTwo");
    }

    @Test
    public void shouldMatchExactClassAndMethodCharacters() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("BasicTest#test???????One")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.BasicTest#testSuccessOne");
    }

    @Test
    public void shouldMatchExactClassAndMethodsPostfix() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("TestFive#testSuccess???")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestFive#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestFive#testSuccessTwo");
    }

    @Test
    public void shouldMatchExactClassAndMethodPostfix() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("TestFive#testSuccess?????")
                .executeTest()
                .verifyErrorFree(1)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestFive#testSuccessThree");
    }

    @Test
    public void shouldMatchExactClassAndMultipleMethods() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("TestFive#testSuccessOne+testSuccessThree")
                .executeTest()
                .verifyErrorFree(2)
                .verifyErrorFreeLog()
                .verifyTextInLog("jiras.surefire745.TestFive#testSuccessOne")
                .verifyTextInLog("jiras.surefire745.TestFive#testSuccessThree");
    }

    @Test
    public void shouldMatchMultiplePatterns() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        String test = "jiras/surefire745/BasicTest#testSuccessOne+testSuccessTwo" // 2
                + ',' + "jiras/**/TestTwo" // 2
                + ',' + "jiras/surefire745/TestThree#testSuccess*" // 2
                + ',' + "TestFour#testSuccess???" // 2
                + ',' + "jiras/surefire745/*Five#test*One"; // 1

        prepare(test).executeTest().verifyErrorFree(9).verifyErrorFreeLog();
    }

    @Test
    public void shouldMatchMultiplePatternsAsParallel() {
        assumeTrue(getSettings().getFramework() == JUNIT4 || getSettings().getFramework() == TestNG);
        assumeTrue(getSettings().getConfiguration() == TEST);
        String test = "jiras/surefire745/BasicTest#testSuccessOne+testSuccessTwo" // 2
                + ',' + "jiras/**/TestTwo" // 2
                + ',' + "jiras/surefire745/TestThree#testSuccess*" // 2
                + ',' + "TestFour#testSuccess???" // 2
                + ',' + "jiras/surefire745/*Five#test*One"; // 1

        prepare(test)
                .parallel("classes")
                .useUnlimitedThreads()
                .executeTest()
                .verifyErrorFree(9)
                .verifyErrorFreeLog();
    }

    @Test
    public void shouldMatchMultiplePatternsComplex() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        String test = "**/BasicTest#testSuccessOne+testSuccessTwo" // 2
                + ',' + "jiras/**/TestTwo" // 2
                + ',' + "?????/surefire745/TestThree#testSuccess*" // 2
                + ',' + "jiras/surefire745/TestFour.java#testSuccess???" // 2
                + ',' + "jiras/surefire745/*Five#test*One"; // 1

        prepare(test).executeTest().verifyErrorFree(9).verifyErrorFreeLog();
    }

    @Test
    public void shouldMatchMultiplePatternsComplexAsParallel() {
        assumeTrue(getSettings().getFramework() == JUNIT4 || getSettings().getFramework() == TestNG);
        assumeTrue(getSettings().getConfiguration() == TEST);
        String test = "**/BasicTest#testSuccessOne+testSuccessTwo" // 2
                + ',' + "jiras/**/TestTwo" // 2
                + ',' + "?????/surefire745/TestThree#testSuccess*" // 2
                + ',' + "jiras/surefire745/TestFour.java#testSuccess???" // 2
                + ',' + "jiras/surefire745/*Five#test*One"; // 1

        prepare(test)
                .parallel("classes")
                .useUnlimitedThreads()
                .executeTest()
                .verifyErrorFree(9)
                .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedClasses() {
        prepare("!BasicTest, !**/TestTwo, !**/TestThree.java")
                .executeTest()
                .verifyErrorFree(6)
                .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedClassesIfIncluded() {
        prepare("TestF*.java, !**/TestFour.java")
                .executeTest()
                .verifyErrorFree(3)
                .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedMethods() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("!#*Fail*, !%regex[#.*One], !#testSuccessThree")
                .executeTest()
                .verifyErrorFree(5)
                .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedClassesAndMethods() {
        assumeTrue(getSettings().getConfiguration() == TEST);
        prepare("!#*Fail*, !TestFour#testSuccessTwo")
                .executeTest()
                .verifyErrorFree(11)
                .verifyErrorFreeLog();
    }

    @Test
    public void negativeTest() {
        assumeTrue(getSettings().getConfiguration() == INCLUDES
                || getSettings().getConfiguration() == INCLUDES_EXCLUDES
                || getSettings().getConfiguration() == INCLUDES_FILE
                || getSettings().getConfiguration() == INCLUDES_EXCLUDES_FILE);
        String pattern = "TestFive#testSuccessOne+testSuccessThree";
        prepare(pattern)
                .failNever()
                .executeTest()
                .verifyTextInLog("Method filter prohibited in includes|excludes parameter: " + pattern);
    }
}
