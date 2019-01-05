package org.apache.maven.surefire.util.internal;

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

import java.util.Objects;

import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;

/**
 * Data transfer object of class and method literals.
 */
public final class ClassMethod
{
    private final String clazz;

    private final String method;

    public ClassMethod( String clazz, String method )
    {
        this.clazz = clazz;
        this.method = method;
    }

    public boolean isValidTest()
    {
        return !isBlank( clazz ) && !isBlank( method );
    }

    public String getClazz()
    {
        return clazz;
    }

    public String getMethod()
    {
        return method;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        ClassMethod that = ( ClassMethod ) o;
        return Objects.equals( clazz, that.clazz )
                && Objects.equals( method, that.method );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( clazz, method );
    }
}
