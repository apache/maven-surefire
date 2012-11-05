package org.apache.maven.surefire.testng.utils;

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

import java.util.List;
import org.apache.maven.shared.utils.io.SelectorUtils;

import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;

/**
 * For internal use only
 *
 * @author Olivier Lamy
 * @noinspection UnusedDeclaration
 * @since 2.7.3
 */
public class MethodSelector
    implements IMethodSelector
{

    private static String METHOD_NAME = null;

    public void setTestMethods( List arg0 )
    {
        // noop                    
    }

    public boolean includeMethod( IMethodSelectorContext context, ITestNGMethod testngMethod, boolean isTestMethod )
    {
        if ( testngMethod.isBeforeClassConfiguration() || testngMethod.isBeforeGroupsConfiguration()
            || testngMethod.isBeforeMethodConfiguration() || testngMethod.isBeforeSuiteConfiguration()
            || testngMethod.isBeforeTestConfiguration() )
        {
            return true;
        }
        if ( testngMethod.isAfterClassConfiguration() || testngMethod.isAfterGroupsConfiguration()
            || testngMethod.isAfterMethodConfiguration() || testngMethod.isAfterSuiteConfiguration()
            || testngMethod.isAfterTestConfiguration() )
        {
            return true;
        }

        return SelectorUtils.match( METHOD_NAME, testngMethod.getMethodName() );
    }

    public static void setMethodName( String methodName )
    {
        METHOD_NAME = methodName;
    }
}
