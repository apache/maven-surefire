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


import org.apache.maven.surefire.api.filter.NonAbstractClassFilter;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.api.util.ScannerFilter;
import org.junit.experimental.runners.Enclosed;


/**
 * Looks for additional junit48-like features
 * @author Geoff Denning
 * @author Kristian Rosenvold
 */
public class JUnit48TestChecker
    implements ScannerFilter
{
    private final NonAbstractClassFilter nonAbstractClassFilter;

    private final JUnit4TestChecker jUnit4TestChecker;


    public JUnit48TestChecker( ClassLoader testClassLoader )
    {
        this.jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );
        this.nonAbstractClassFilter = new NonAbstractClassFilter();
    }

    @Override
    public boolean accept( Class testClass )
    {
        return jUnit4TestChecker.accept( testClass ) || isAbstractWithEnclosedRunner( testClass );
    }

    @SuppressWarnings( { "unchecked" } )
    private boolean isAbstractWithEnclosedRunner( Class testClass )
    {
        return jUnit4TestChecker.isRunWithPresentInClassLoader()
                        &&  isAbstract( testClass )
                        && isRunWithEnclosedRunner( testClass );
    }

    private boolean isRunWithEnclosedRunner( Class testClass )
    {
        @SuppressWarnings( "unchecked" ) org.junit.runner.RunWith runWithAnnotation =
            (org.junit.runner.RunWith) testClass.getAnnotation( org.junit.runner.RunWith.class );
        return ( runWithAnnotation != null && Enclosed.class.equals( runWithAnnotation.value() ) );
    }

    private boolean isAbstract( Class testClass )
    {
        return !nonAbstractClassFilter.accept( testClass );
    }
}
