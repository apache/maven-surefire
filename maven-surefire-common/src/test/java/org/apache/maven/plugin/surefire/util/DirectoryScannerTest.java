package org.apache.maven.plugin.surefire.util;

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

import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.ScanResult;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.runners.Parameterized.*;

/**
 * @author Kristian Rosenvold
 */
@RunWith( Parameterized.class )
public class DirectoryScannerTest
{
    @Parameters( name = "\"{0}\" should count {1} classes" )
    public static Iterable<Object[]> data() {
        return Arrays.asList( new Object[][] {
            { "**/*ZT*A.java", is( 3 ) },
            { "**/*ZT*A.java#testMethod", is( 3 ) },
            { "**/*ZT?A.java#testMethod, !*ZT2A", is( 2 ) },
            { "**/*ZT?A.java#testMethod, !*ZT2A#testMethod", is( 3 ) },
            { "#testMethod", is( greaterThanOrEqualTo( 3 ) ) },
        } );
    }

    @Parameter( 0 )
    public String filter;

    @Parameter( 1 )
    public Matcher<? super Integer> expectedClassesCount;

    @Test
    public void locateTestClasses()
        throws Exception
    {
        // use target as people can configure ide to compile in an other place than maven
        File baseDir = new File( new File( "target/test-classes" ).getCanonicalPath() );
        TestListResolver resolver = new TestListResolver( filter );
        DirectoryScanner surefireDirectoryScanner = new DirectoryScanner( baseDir, resolver );

        ScanResult classNames = surefireDirectoryScanner.scan();
        assertThat( classNames, is( notNullValue() ) );
        assertThat( classNames.size(), is( expectedClassesCount ) );

        Map<String, String> props = new HashMap<>();
        classNames.writeTo( props );
        assertThat( props.values(), hasSize( expectedClassesCount ) );
    }
}
