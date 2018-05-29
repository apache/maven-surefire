package junit5;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test using the JUnit 5 API, which should be executed by JUnit jupiter enigne
 */
public class JUnit5Test
{
    private static boolean tearDownCalled;

    private boolean setUpCalled;

    @BeforeEach
    public void setUp()
    {
        setUpCalled = true;
        System.out.println( "Called setUp" );
    }

    @AfterEach
    public void tearDown()
    {
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        assertTrue( setUpCalled, "setUp was not called" );
    }

    @AfterAll
    public static void oneTimeTearDown()
    {
        assertTrue( tearDownCalled, "tearDown was not called" );
    }

}
