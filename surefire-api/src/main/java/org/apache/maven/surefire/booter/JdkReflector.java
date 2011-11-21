package org.apache.maven.surefire.booter;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.ReflectionUtils;

/**
 * Does reflection based invocation of misc jdk method
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class JdkReflector
{
    private final Method assertionStatusMethod;


    public JdkReflector()
    {
        assertionStatusMethod = ReflectionUtils.tryGetMethod( ClassLoader.class, "setDefaultAssertionStatus",
                                                              new Class[]{ boolean.class } );
    }

    public void invokeAssertionStatusMethod( ClassLoader classLoader, boolean enableAssertions )
    {
        if ( assertionStatusMethod != null )
        {
            try
            {
                Object[] args = new Object[]{ enableAssertions ? Boolean.TRUE : Boolean.FALSE };
                assertionStatusMethod.invoke( classLoader, args );
            }
            catch ( IllegalAccessException e )
            {
                throw new NestedRuntimeException( "Unable to access the assertion enablement method", e );
            }
            catch ( InvocationTargetException e )
            {
                throw new NestedRuntimeException( "Unable to invoke the assertion enablement method", e );
            }
        }
    }

}
