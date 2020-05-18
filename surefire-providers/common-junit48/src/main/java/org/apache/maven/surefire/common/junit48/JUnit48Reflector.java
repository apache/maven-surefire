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

import static org.apache.maven.surefire.api.util.ReflectionUtils.tryLoadClass;

/**
 * @author Kristian Rosenvold
 */
public final class JUnit48Reflector
{
    private static final String CATEGORIES = "org.junit.experimental.categories.Categories";

    private static final String CATEGORY = "org.junit.experimental.categories.Category";

    private final Class<?> categories;

    private final Class<?> category;

    public JUnit48Reflector( ClassLoader testClassLoader )
    {
        categories = tryLoadClass( testClassLoader, CATEGORIES );
        category = tryLoadClass( testClassLoader, CATEGORY );
    }

    public boolean isJUnit48Available()
    {
        return categories != null;
    }

    boolean isCategoryAnnotationPresent( Class clazz )
    {
        return clazz != null && category != null
               && ( clazz.getAnnotation( category ) != null || isCategoryAnnotationPresent( clazz.getSuperclass() ) );
    }
}
