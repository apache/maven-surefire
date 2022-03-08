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

import org.apache.maven.surefire.its.fixture.Settings;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.TestFramework.JUNIT47;
import static org.apache.maven.surefire.its.fixture.TestFramework.TestNG;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES_EXCLUDES;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES_EXCLUDES_FILE;
import static org.apache.maven.surefire.its.fixture.Configuration.INCLUDES_FILE;
import static org.apache.maven.surefire.its.fixture.Configuration.TEST;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

/**
 * Test project using multiple method patterns, including wildcards in class and method names.
 */
public abstract class AbstractTestMultipleMethodPatterns
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String CSV_DELIMITER_SHORT = ",";
    private static final String CSV_DELIMITER_LONG = ", ";
    private static final String NOT_DELIMITER = "!";

    protected abstract Settings getSettings();

    protected abstract SurefireLauncher unpack();

    protected SurefireLauncher prepare( String tests )
    {
        SurefireLauncher launcher = unpack().addGoal( "-P " + getSettings().profile() );
        String[] includedExcluded = splitIncludesExcludes( tests );
        switch ( getSettings().getConfiguration() )
        {
            case TEST:
                launcher.setTestToRun( tests );
                break;
            case INCLUDES:
                launcher.sysProp( "included", tests );
                break;
            case INCLUDES_EXCLUDES:
                launcher.sysProp( "included", includedExcluded[0] );
                launcher.sysProp( "excluded", includedExcluded[1] );
                break;
            default:
                throw new IllegalArgumentException( "Unsupported configuration " + getSettings().getConfiguration() );
        }
        return launcher;
    }

    private static String[] splitIncludesExcludes( String patterns )
    {
        String included = "";
        String excluded = "";
        for ( String pattern : patterns.split( CSV_DELIMITER_SHORT ) )
        {
            pattern = pattern.trim();
            if ( pattern.startsWith( NOT_DELIMITER ) )
            {
                excluded += pattern.substring( NOT_DELIMITER.length() ).trim();
                excluded += CSV_DELIMITER_LONG;
            }
            else
            {
                included += pattern;
                included += CSV_DELIMITER_LONG;
            }
        }
        return new String[]{ trimEndComma( included ), trimEndComma( excluded ) };
    }

    private static String trimEndComma( String pattern )
    {
        pattern = pattern.trim();
        return pattern.endsWith( CSV_DELIMITER_LONG )
            ? pattern.substring( 0, pattern.length() - CSV_DELIMITER_LONG.length() ) : pattern;
    }

    @Test
    public void simpleNameTest()
        throws Exception
    {
        prepare( "TestTwo" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void simpleNameTestAsParallel()
        throws Exception
    {
        assumeThat( getSettings().getFramework(), anyOf( is( JUNIT47 ), is( TestNG ) ) );
        prepare( "TestTwo" )
            .parallel( "classes" )
            .useUnlimitedThreads()
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void simpleNameTestWithJavaExt()
        throws Exception
    {
        prepare( "TestTwo.java" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void simpleNameTestWithWildcardPkg()
        throws Exception
    {
        prepare( "**/TestTwo" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void simpleNameTestWithJavaExtWildcardPkg()
        throws Exception
    {
        prepare( "**/TestTwo.java" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void fullyQualifiedTest()
        throws Exception
    {
        prepare( "jiras/surefire745/TestTwo.java" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void shouldMatchSimpleClassNameAndMethod()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "BasicTest#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    /**
     * This method name was shorten because it cause 261 character long path on Windows with Jenkins Pipeline.
     */
    @Test
    public void matchSimpleClassAndMethodWithJavaExt()
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "BasicTest.java#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    /**
     * This method name was shorten because it cause 261 character long path on Windows with Jenkins Pipeline.
     */
    @Test
    public void matchSimpleClassAndMethodWithWildcardPkg()
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "**/BasicTest#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    /**
     * This method name was shorten because it cause 261 character long path on Windows with Jenkins Pipeline.
     */
    @Test
    public void matchSimpleClassAndMethodWithJavaExtWildcardPkg()
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "**/BasicTest.java#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void shouldMatchWildcardPackageAndClassAndMethod()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "jiras/**/BasicTest#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void regexClass()
        throws Exception
    {
        prepare( "%regex[.*.TestTwo.*]" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestTwo#testSuccessTwo" );
    }

    @Test
    public void testSuccessTwo()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "#testSuccessTwo" )
            .maven().debugLogging()
            .executeTest()
            .verifyErrorFree( 5 )
            .verifyErrorFreeLog();
    }

    @Test
    public void testRegexSuccessTwo()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "%regex[#testSuccessTwo]" )
            .executeTest()
            .verifyErrorFree( 5 )
            .verifyErrorFreeLog();
    }

    @Test
    public void regexClassAndMethod()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "%regex[.*.BasicTest.*#testSuccessTwo]" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void shouldMatchExactClassAndMethodWildcard()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "BasicTest#test*One" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessOne" );
    }

    @Test
    public void shouldMatchExactClassAndMethodsWildcard()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "BasicTest#testSuccess*" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void shouldMatchExactClassAndMethodCharacters()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "BasicTest#test???????One" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessOne" );
    }

    @Test
    public void shouldMatchExactClassAndMethodsPostfix()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "TestFive#testSuccess???" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessTwo" );
    }

    @Test
    public void shouldMatchExactClassAndMethodPostfix()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "TestFive#testSuccess?????" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessThree" );
    }

    @Test
    public void shouldMatchExactClassAndMultipleMethods()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "TestFive#testSuccessOne+testSuccessThree" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessThree" );
    }

    @Test
    public void shouldMatchMultiplePatterns()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        String test = "jiras/surefire745/BasicTest#testSuccessOne+testSuccessTwo" //2
            + ',' + "jiras/**/TestTwo" //2
            + ',' + "jiras/surefire745/TestThree#testSuccess*" //2
            + ',' + "TestFour#testSuccess???" //2
            + ',' + "jiras/surefire745/*Five#test*One"; //1

        prepare( test )
            .executeTest()
            .verifyErrorFree( 9 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldMatchMultiplePatternsAsParallel()
        throws Exception
    {
        assumeThat( getSettings().getFramework(), anyOf( is( JUNIT47 ), is( TestNG ) ) );
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        String test = "jiras/surefire745/BasicTest#testSuccessOne+testSuccessTwo" //2
            + ',' + "jiras/**/TestTwo" //2
            + ',' + "jiras/surefire745/TestThree#testSuccess*" //2
            + ',' + "TestFour#testSuccess???" //2
            + ',' + "jiras/surefire745/*Five#test*One"; //1

        prepare( test )
            .parallel( "classes" )
            .useUnlimitedThreads()
            .executeTest()
            .verifyErrorFree( 9 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldMatchMultiplePatternsComplex()
        throws Exception
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        String test = "**/BasicTest#testSuccessOne+testSuccessTwo" //2
            + ',' + "jiras/**/TestTwo" //2
            + ',' + "?????/surefire745/TestThree#testSuccess*" //2
            + ',' + "jiras/surefire745/TestFour.java#testSuccess???" //2
            + ',' + "jiras/surefire745/*Five#test*One"; //1

        prepare( test )
            .executeTest()
            .verifyErrorFree( 9 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldMatchMultiplePatternsComplexAsParallel()
        throws Exception
    {
        assumeThat( getSettings().getFramework(), anyOf( is( JUNIT47 ), is( TestNG ) ) );
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        String test = "**/BasicTest#testSuccessOne+testSuccessTwo" //2
            + ',' + "jiras/**/TestTwo" //2
            + ',' + "?????/surefire745/TestThree#testSuccess*" //2
            + ',' + "jiras/surefire745/TestFour.java#testSuccess???" //2
            + ',' + "jiras/surefire745/*Five#test*One"; //1

        prepare( test )
            .parallel( "classes" )
            .useUnlimitedThreads()
            .executeTest()
            .verifyErrorFree( 9 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedClasses()
    {
        prepare( "!BasicTest, !**/TestTwo, !**/TestThree.java" )
            .executeTest()
            .verifyErrorFree( 6 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedClassesIfIncluded()
    {
        prepare( "TestF*.java, !**/TestFour.java" )
            .executeTest()
            .verifyErrorFree( 3 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedMethods()
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "!#*Fail*, !%regex[#.*One], !#testSuccessThree" )
            .executeTest()
            .verifyErrorFree( 5 )
            .verifyErrorFreeLog();
    }

    @Test
    public void shouldNotRunExcludedClassesAndMethods()
    {
        assumeThat( getSettings().getConfiguration(), is( TEST ) );
        prepare( "!#*Fail*, !TestFour#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 11 )
            .verifyErrorFreeLog();
    }

    @Test
    public void negativeTest()
    {
        assumeThat( getSettings().getConfiguration(), anyOf( is( INCLUDES ), is( INCLUDES_EXCLUDES ),
                                                             is( INCLUDES_FILE ), is( INCLUDES_EXCLUDES_FILE ) ) );
        String pattern = "TestFive#testSuccessOne+testSuccessThree";
        prepare( pattern )
            .failNever()
            .executeTest()
            .verifyTextInLog( "Method filter prohibited in includes|excludes parameter: " + pattern );
    }
}
