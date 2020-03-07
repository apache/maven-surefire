package org.apache.maven.plugins.surefire.report;

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

/**
 *
 */
public final class ReportTestSuite
{
    private final List<ReportTestCase> testCases = new ArrayList<>();

    private int numberOfErrors;

    private int numberOfFailures;

    private int numberOfSkipped;

    private int numberOfFlakes;

    private Integer numberOfTests;

    private String name;

    private String fullClassName;

    private String packageName;

    private float timeElapsed;

    public List<ReportTestCase> getTestCases()
    {
        return testCases;
    }

    public int getNumberOfErrors()
    {
        return numberOfErrors;
    }

    public ReportTestSuite setNumberOfErrors( int numberOfErrors )
    {
        this.numberOfErrors = numberOfErrors;
        return this;
    }

    public ReportTestSuite incrementNumberOfErrors()
    {
        ++numberOfErrors;
        return this;
    }

    public int getNumberOfFailures()
    {
        return numberOfFailures;
    }

    public ReportTestSuite setNumberOfFailures( int numberOfFailures )
    {
        this.numberOfFailures = numberOfFailures;
        return this;
    }

    public ReportTestSuite incrementNumberOfFailures()
    {
        ++numberOfFailures;
        return this;
    }

    public int getNumberOfSkipped()
    {
        return numberOfSkipped;
    }

    public ReportTestSuite setNumberOfSkipped( int numberOfSkipped )
    {
        this.numberOfSkipped = numberOfSkipped;
        return this;
    }

    public ReportTestSuite incrementNumberOfSkipped()
    {
        ++numberOfSkipped;
        return this;
    }

    public int getNumberOfFlakes()
    {
        return numberOfFlakes;
    }

    public ReportTestSuite setNumberOfFlakes( int numberOfFlakes )
    {
        this.numberOfFlakes = numberOfFlakes;
        return this;
    }

    public ReportTestSuite incrementNumberOfFlakes()
    {
        ++numberOfFlakes;
        return this;
    }

    public int getNumberOfTests()
    {
        return numberOfTests == null ? testCases.size() : numberOfTests;
    }

    public ReportTestSuite setNumberOfTests( int numberOfTests )
    {
        this.numberOfTests = numberOfTests;
        return this;
    }

    public String getName()
    {
        return name;
    }

    public ReportTestSuite setName( String name )
    {
        this.name = name;
        return this;
    }

    public String getFullClassName()
    {
        return fullClassName;
    }

    public ReportTestSuite setFullClassName( String fullClassName )
    {
        this.fullClassName = fullClassName;
        int lastDotPosition = fullClassName.lastIndexOf( "." );
        name = fullClassName.substring( lastDotPosition + 1, fullClassName.length() );
        packageName = lastDotPosition == -1 ? "" : fullClassName.substring( 0, lastDotPosition );
        return this;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public ReportTestSuite setPackageName( String packageName )
    {
        this.packageName = packageName;
        return this;
    }

    public float getTimeElapsed()
    {
        return this.timeElapsed;
    }

    public ReportTestSuite setTimeElapsed( float timeElapsed )
    {
        this.timeElapsed = timeElapsed;
        return this;
    }

    ReportTestSuite setTestCases( List<ReportTestCase> testCases )
    {
        this.testCases.clear();
        this.testCases.addAll( testCases );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return fullClassName + " [" + getNumberOfTests() + "/" + getNumberOfFailures() + "/"
            + getNumberOfErrors() + "/" + getNumberOfSkipped() + "]";
    }
}
