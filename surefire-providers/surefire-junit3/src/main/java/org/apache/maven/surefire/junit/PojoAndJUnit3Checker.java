package org.apache.maven.surefire.junit;

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

import org.apache.maven.surefire.api.filter.NonAbstractClassFilter;
import org.apache.maven.surefire.common.junit3.JUnit3TestChecker;
import org.apache.maven.surefire.api.util.ScannerFilter;

/**
 * @author Kristian Rosenvold
 */
public class PojoAndJUnit3Checker
    implements ScannerFilter
{
    private final JUnit3TestChecker jUnit3TestChecker;

    private final NonAbstractClassFilter nonAbstractClassFilter = new NonAbstractClassFilter();

    public PojoAndJUnit3Checker( JUnit3TestChecker jUnit3TestChecker )
    {
        this.jUnit3TestChecker = jUnit3TestChecker;
    }

    @Override
    public boolean accept( Class testClass )
    {
        return jUnit3TestChecker.accept( testClass )
            || nonAbstractClassFilter.accept( testClass ) && isPojoTest( testClass );
    }

    private boolean isPojoTest( Class<?> testClass )
    {
        try
        {
            testClass.getConstructor();
            return true;
        }
        catch ( Exception e )
        {
            return false;
        }
    }

}
