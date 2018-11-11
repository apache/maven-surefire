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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.surefire.util.ReflectionUtils;

import org.junit.runner.notification.RunListener;

import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4RunListenerFactory
{
    public static List<RunListener> createCustomListeners( String listeners )
    {
        List<RunListener> result = new ArrayList<>();
        if ( isNotBlank( listeners ) )
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            for ( String listener : listeners.split( "," ) )
            {
                if ( isNotBlank( listener ) )
                {
                    result.add( ReflectionUtils.instantiate( cl, listener, RunListener.class ) );
                }
            }
        }
        return result;
    }

}
