package testng.jdk14;

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

import org.testng.Assert;

/**
 * Tests that forcing testng to run tests via the <code>"${maven.test.forcetestng}"</code> configuration option works.
 * 
 * @author jkuhnert
 */
public class TestNGJavadocTest
{

    /**
     * Sets up testObject
     * 
     * @testng.configuration beforeTestClass = "true" groups = "functional"
     */
    public void configureTest()
    {
        testObject = new Object();
    }

    Object testObject;

    /**
     * Tests reporting an error
     * 
     * @testng.test groups = "functional, notincluded"
     */
    public void isTestObjectNull()
    {
        Assert.assertNotNull( testObject, "testObject is null" );
    }

    /**
     * Sample method that shouldn't be run by test suite.
     * 
     * @testng.test groups = "notincluded"
     */
    public void shouldNotRun()
    {
        Assert.assertTrue( false, "Group specified by test shouldnt be run." );
    }
}