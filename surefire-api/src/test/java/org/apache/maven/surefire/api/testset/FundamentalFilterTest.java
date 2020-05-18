package org.apache.maven.surefire.api.testset;

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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings( { "javadoc", "checkstyle:javadoctype" } )
/**
 * Inclusive test patters:<p>
 *
 * <table cellspacing=0 border=1>
 * <tr>
 * <td style=min-width:50px> test</td>
 * <td style=min-width:50px></td>
 * <td style=min-width:50px> pattern</td>
 * <td style=min-width:50px></td>
 * <td style=min-width:50px></td>
 * <td style=min-width:50px></td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>class</td>
 * <td style=min-width:50px>method</td>
 * <td style=min-width:50px>class</td>
 * <td style=min-width:50px>method</td>
 * <td style=min-width:50px>shouldRunAsInclusive</td>
 * <td style=min-width:50px></td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y (wildcard pattern)</td>
 * <td style=min-width:50px>testIncludes1</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y (suppose suite and custome filter)</td>
 * <td style=min-width:50px>testIncludes2</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y (suppose Suite)</td>
 * <td style=min-width:50px>testIncludes3</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y (suppose Suite)</td>
 * <td style=min-width:50px>testIncludes4</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y (wildcard pattern)</td>
 * <td style=min-width:50px>testIncludes5</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match methods</td>
 * <td style=min-width:50px>testIncludes6</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y (due to Cucumber)</td>
 * <td style=min-width:50px>testIncludes7</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y (due to Cucumber)</td>
 * <td style=min-width:50px>testIncludes8</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y (wildcard pattern)</td>
 * <td style=min-width:50px>testIncludes9</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y (suppose suite and custome filter)</td>
 * <td style=min-width:50px>testIncludes10</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>match classes</td>
 * <td style=min-width:50px>testIncludes11</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match classes</td>
 * <td style=min-width:50px>testIncludes12</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y (wildcard pattern)</td>
 * <td style=min-width:50px>testIncludes13</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match methods</td>
 * <td style=min-width:50px>testIncludes14</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>match classes</td>
 * <td style=min-width:50px>testIncludes15</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match all</td>
 * <td style=min-width:50px>testIncludes16</td>
 * </tr>
 * </table>
 * <p>
 * <p>
 * Exclusive test patters:<p>
 *
 * <table cellspacing=0 border=1>
 * <tr>
 * <td style=min-width:50px> test</td>
 * <td style=min-width:50px></td>
 * <td style=min-width:50px> pattern</td>
 * <td style=min-width:50px></td>
 * <td style=min-width:50px></td>
 * <td style=min-width:50px></td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>class</td>
 * <td style=min-width:50px>method</td>
 * <td style=min-width:50px>class</td>
 * <td style=min-width:50px>method</td>
 * <td style=min-width:50px>shouldRunAsExclusive</td>
 * <td style=min-width:50px></td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n (wildcard pattern)</td>
 * <td style=min-width:50px>testExcludes1</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n (suppose suite and custome filter)</td>
 * <td style=min-width:50px>testExcludes2</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n (suppose Suite)</td>
 * <td style=min-width:50px>testExcludes3</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n (suppose Suite)</td>
 * <td style=min-width:50px>testExcludes4</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n (wildcard pattern)</td>
 * <td style=min-width:50px>testExcludes5</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match methods</td>
 * <td style=min-width:50px>testExcludes6</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n (due to Cucumber)</td>
 * <td style=min-width:50px>testExcludes7</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n (due to Cucumber)</td>
 * <td style=min-width:50px>testExcludes8</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n (wildcard pattern)</td>
 * <td style=min-width:50px>testExcludes9</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n (suppose suite and custome filter)</td>
 * <td style=min-width:50px>testExcludes10</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>match classes</td>
 * <td style=min-width:50px>testExcludes11</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n (cannot exclude in dir.scanner)</td>
 * <td style=min-width:50px>testExcludes12</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>n (wildcard pattern)</td>
 * <td style=min-width:50px>testExcludes13</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match methods</td>
 * <td style=min-width:50px>testExcludes14</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>n</td>
 * <td style=min-width:50px>match classes</td>
 * <td style=min-width:50px>testExcludes15</td>
 * </tr>
 * <tr>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>y</td>
 * <td style=min-width:50px>match all</td>
 * <td style=min-width:50px>testExcludes16</td>
 * </tr>
 * </table>
 */
public class FundamentalFilterTest
{
    @Test
    public void testIncludes1()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsInclusive( null, null ), is( true ) );
    }

    @Test
    public void testIncludes2()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsInclusive( null, null ), is( true ) );
    }

    @Test
    public void testIncludes3()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsInclusive( null, null ), is( true ) );
    }

    @Test
    public void testIncludes4()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsInclusive( null, null ), is( true ) );
    }

    @Test
    public void testIncludes5()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsInclusive( null, "method" ), is( true ) );
    }

    @Test
    public void testIncludes6()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsInclusive( null, "method" ), is( true ) );
        assertThat( pattern.matchAsInclusive( null, "otherMethod" ), is( false ) );
    }

    /**
     * Does not throw NPE due to Cucumber has test class NULL and test method NOT NULL.
     */
    @Test
    public void testIncludes7()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsInclusive( null, "method" ), is( true ) );
    }

    /**
     * Does not throw NPE due to Cucumber has test class NULL and test method NOT NULL.
     */
    @Test
    public void testIncludes8()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsInclusive( null, "method" ), is( true ) );
        assertThat( pattern.matchAsInclusive( null, "otherMethod" ), is( true ) );
    }

    @Test
    public void testIncludes9()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsInclusive( "Test.class", null ), is( true ) );
    }

    @Test
    public void testIncludes10()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsInclusive( "Test.class", null ), is( true ) );
    }

    @Test
    public void testIncludes11()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsInclusive( "Test.class", null ), is( true ) );
        assertThat( pattern.matchAsInclusive( "Other.class", null ), is( false ) );
    }

    @Test
    public void testIncludes12()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsInclusive( "Test.class", null ), is( true ) );
        assertThat( pattern.matchAsInclusive( "Other.class", null ), is( false ) );
    }

    @Test
    public void testIncludes13()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsInclusive( "Test.class", "method" ), is( true ) );
    }

    @Test
    public void testIncludes14()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsInclusive( "Test.class", "method" ), is( true ) );
        assertThat( pattern.matchAsInclusive( "Test.class", "otherMethod" ), is( false ) );
    }

    @Test
    public void testIncludes15()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsInclusive( "Test.class", "method" ), is( true ) );
        assertThat( pattern.matchAsInclusive( "Other.class", "method" ), is( false ) );
    }

    @Test
    public void testIncludes16()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsInclusive( "Test.class", "method" ), is( true ) );
        assertThat( pattern.matchAsInclusive( "Test.class", "otherMethod" ), is( false ) );
        assertThat( pattern.matchAsInclusive( "Other.class", "method" ), is( false ) );
        assertThat( pattern.matchAsInclusive( "Other.class", "otherMethod" ), is( false ) );
    }

    @Test
    public void testExcludes1()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsExclusive( null, null ), is( false ) );
    }

    @Test
    public void testExcludes2()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsExclusive( null, null ), is( false ) );
    }

    @Test
    public void testExcludes3()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsExclusive( null, null ), is( false ) );
    }

    @Test
    public void testExcludes4()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsExclusive( null, null ), is( false ) );
    }

    @Test
    public void testExcludes5()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsExclusive( null, "method" ), is( false ) );
    }

    @Test
    public void testExcludes6()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsExclusive( null, "method" ), is( true ) );
        assertThat( pattern.matchAsExclusive( null, "otherMethod" ), is( false ) );
    }

    /**
     * Does not throw NPE due to Cucumber has test class NULL and test method NOT NULL.
     */
    @Test
    public void testExcludes7()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsExclusive( null, "method" ), is( false ) );
    }

    /**
     * Does not throw NPE due to Cucumber has test class NULL and test method NOT NULL.
     */
    @Test
    public void testExcludes8()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsExclusive( null, "method" ), is( false ) );
        assertThat( pattern.matchAsExclusive( null, "otherMethod" ), is( false ) );
    }

    @Test
    public void testExcludes9()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsExclusive( "Test.class", null ), is( false ) );
    }

    @Test
    public void testExcludes10()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsExclusive( "Test.class", null ), is( false ) );
    }

    @Test
    public void testExcludes11()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsExclusive( "Test.class", null ), is( true ) );
        assertThat( pattern.matchAsExclusive( "Other.class", null ), is( false ) );
    }

    @Test
    public void testExcludes12()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsExclusive( "Test.class", null ), is( false ) );
        assertThat( pattern.matchAsExclusive( "Other.class", null ), is( false ) );
    }

    @Test
    public void testExcludes13()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, null, false );
        assertThat( pattern.matchAsExclusive( "Test.class", "method" ), is( false ) );
    }

    @Test
    public void testExcludes14()
    {
        ResolvedTest pattern = new ResolvedTest( (String) null, "method", false );
        assertThat( pattern.matchAsExclusive( "Test.class", "method" ), is( true ) );
        assertThat( pattern.matchAsExclusive( "Test.class", "otherMethod" ), is( false ) );
    }

    @Test
    public void testExcludes15()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", null, false );
        assertThat( pattern.matchAsExclusive( "Test.class", "method" ), is( true ) );
        assertThat( pattern.matchAsExclusive( "Other.class", "method" ), is( false ) );
    }

    @Test
    public void testExcludes16()
    {
        ResolvedTest pattern = new ResolvedTest( "Test", "method", false );
        assertThat( pattern.matchAsExclusive( "Test.class", "method" ), is( true ) );
        assertThat( pattern.matchAsExclusive( "Test.class", "otherMethod" ), is( false ) );
        assertThat( pattern.matchAsExclusive( "Other.class", "method" ), is( false ) );
        assertThat( pattern.matchAsExclusive( "Other.class", "otherMethod" ), is( false ) );
    }
}
