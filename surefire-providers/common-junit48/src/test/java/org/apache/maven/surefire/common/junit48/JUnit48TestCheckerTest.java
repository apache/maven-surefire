package org.apache.maven.surefire.common.junit48;

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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class JUnit48TestCheckerTest
{
    @Test
    public void valid48Class()
    {
        JUnit48TestChecker tc = new JUnit48TestChecker( this.getClass().getClassLoader() );
        assertTrue( tc.accept( BasicTest.class ) );
    }

    @Test
    public void notValid48Class()
    {
        JUnit48TestChecker tc = new JUnit48TestChecker( this.getClass().getClassLoader() );
        assertFalse( tc.accept( BasicTest2.class ) );
    }

    /**
     *
     */
    @RunWith( Enclosed.class )
    public abstract static class BasicTest
    {
        /**
         *
         */
        public static class InnerTest
        {
            @Test
            public void testSomething()
            {
            }
        }
    }

    /**
     *
     */
    @RunWith( Parameterized.class )
    public abstract static class BasicTest2
    {
        /**
         *
         */
        public static class InnerTest
        {
            @Test
            public void testSomething()
            {
            }
        }
    }
}
