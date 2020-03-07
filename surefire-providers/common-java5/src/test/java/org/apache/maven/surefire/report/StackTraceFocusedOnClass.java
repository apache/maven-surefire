package org.apache.maven.surefire.report;

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

import java.io.IOException;

final class StackTraceFocusedOnClass
{
    static class JRE
    {
        void throwException()
            throws IOException
        {
            throw new IOException( "I/O error" );
        }
    }

    abstract static class A
    {
        abstract void abs()
            throws IOException;

        void a()
        {
            try
            {
                abs();
            }
            catch ( Exception e )
            {
                throw new IllegalStateException( e );
            }
        }
    }

    static class B extends A
    {
        private final JRE jre = new JRE();

        void b()
        {
            try
            {
                a();
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }

        @Override
        void abs()
            throws IOException
        {
            jre.throwException();
        }
    }

    static class C
    {
        private final B b = new B();

        void c()
        {
            b.b();
        }
    }
}
