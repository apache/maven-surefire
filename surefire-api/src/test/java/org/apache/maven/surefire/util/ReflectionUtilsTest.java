package org.apache.maven.surefire.util;

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

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit test for {@link ReflectionUtils}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class ReflectionUtilsTest
{
    @Test(expected = RuntimeException.class)
    public void shouldNotInvokeStaticMethod()
    {
        ReflectionUtils.invokeStaticMethod( ReflectionUtilsTest.class, "notCallable" );
    }

    @Test
    public void shouldInvokeStaticMethod()
    {
        Object o = ReflectionUtils.invokeStaticMethod( ReflectionUtilsTest.class, "callable" );
        assertThat( o )
                .isEqualTo( 3L );
    }

    @Test
    public void shouldInvokeMethodChain()
    {
        Class<?>[] classes1 = { A.class, A.class };
        String[] chain = { "current", "pid" };
        Object o = ReflectionUtils.invokeMethodChain( classes1, chain, null );
        assertThat( o )
                .isEqualTo( 3L );

        Class<?>[] classes2 = { A.class, A.class, B.class };
        String[] longChain = { "current", "createB", "pid" };
        o = ReflectionUtils.invokeMethodChain( classes2, longChain, null );
        assertThat( o )
                .isEqualTo( 1L );
    }

    @Test
    public void shouldInvokeFallbackOnMethodChain()
    {
        Class<?>[] classes1 = { A.class, A.class };
        String[] chain = { "current", "abc" };
        Object o = ReflectionUtils.invokeMethodChain( classes1, chain, 5L );
        assertThat( o )
                .isEqualTo( 5L );

        Class<?>[] classes2 = { A.class, B.class, B.class };
        String[] longChain = { "current", "createB", "abc" };
        o = ReflectionUtils.invokeMethodChain( classes2, longChain, 6L );
        assertThat( o )
                .isEqualTo( 6L );
    }

    private static void notCallable()
    {
    }

    public static long callable()
    {
        return 3L;
    }

    public static class A
    {
        public static A current()
        {
            return new A();
        }

        public long pid()
        {
            return 3L;
        }

        public B createB()
        {
            return new B();
        }
    }

    public static class B
    {
        public long pid()
        {
            return 1L;
        }
    }
}
