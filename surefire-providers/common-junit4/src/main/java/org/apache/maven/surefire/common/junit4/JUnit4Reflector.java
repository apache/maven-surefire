package org.apache.maven.surefire.common.junit4;

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

import java.lang.reflect.Method;
import org.apache.maven.surefire.util.ReflectionUtils;

import org.junit.Ignore;
import org.junit.runner.Description;

public final class JUnit4Reflector
{
    private static final Class[] params = new Class[]{ Class.class };

    private static final Class[] ignoreParams = new Class[]{ Ignore.class };

    public Ignore getAnnotatedIgnore( Description description )
    {
        Method getAnnotation = ReflectionUtils.tryGetMethod( description.getClass(), "getAnnotation", params );

        if ( getAnnotation == null )
        {
            return null;
        }

        return (Ignore) ReflectionUtils.invokeMethodWithArray( description, getAnnotation, ignoreParams );
    }

    public String getAnnotatedIgnoreValue( Description description )
    {
        final Ignore ignore = getAnnotatedIgnore( description );
        return ignore != null ? ignore.value() : null;
    }

}
