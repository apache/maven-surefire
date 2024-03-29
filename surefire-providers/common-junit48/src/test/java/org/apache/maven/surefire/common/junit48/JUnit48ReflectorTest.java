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
package org.apache.maven.surefire.common.junit48;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;

/**
 * @author Kristian Rosenvold
 */
public class JUnit48ReflectorTest extends TestCase {
    public void testIsJUnit48Available() {
        JUnit48Reflector jUnit48Reflector = new JUnit48Reflector(getClass().getClassLoader());
        assertTrue(jUnit48Reflector.isJUnit48Available());
    }

    public void testCategoryAnnotation() {
        JUnit48Reflector jUnit48Reflector = new JUnit48Reflector(getClass().getClassLoader());
        assertTrue(jUnit48Reflector.isCategoryAnnotationPresent(Test1.class));
        assertTrue(jUnit48Reflector.isCategoryAnnotationPresent(Test3.class));
        assertFalse(jUnit48Reflector.isCategoryAnnotationPresent(Test2.class));
    }

    interface Foo {}

    @Category(Foo.class)
    private class Test1 {}

    private class Test2 {}

    private class Test3 extends Test1 {}
}
