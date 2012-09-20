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
public class ReportTestSuite
{
    private List<ReportTestCase> testCases = new ArrayList<ReportTestCase>();

    private int numberOfErrors;

    private int numberOfFailures;

    private int numberOfSkipped;

    private Integer numberOfTests;

    private String name;

    private String fullClassName;

    private String packageName;

    private float timeElapsed;

    public List<ReportTestCase> getTestCases()
    {
        return this.testCases;
    }

    public int getNumberOfErrors()
    {
        return numberOfErrors;
    }

    public void setNumberOfErrors( int numberOfErrors )
    {
        this.numberOfErrors = numberOfErrors;
    }

    public int getNumberOfFailures()
    {
        return numberOfFailures;
    }

    public void setNumberOfFailures( int numberOfFailures )
    {
        this.numberOfFailures = numberOfFailures;
    }

    public int getNumberOfSkipped()
    {
        return numberOfSkipped;
    }

    public void setNumberOfSkipped( int numberOfSkipped )
    {
        this.numberOfSkipped = numberOfSkipped;
    }

    public int getNumberOfTests()
    {
        if ( numberOfTests != null )
        {
            return numberOfTests;
        }
        if ( testCases != null )
        {
            return testCases.size();
        }
        return 0;
    }

    public void setNumberOfTests( int numberOfTests )
    {
        this.numberOfTests = numberOfTests;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getFullClassName()
    {
        return fullClassName;
    }

    public void setFullClassName( String fullClassName )
    {
        this.fullClassName = fullClassName;
        int lastDotPosition = fullClassName.lastIndexOf( "." );

        name = fullClassName.substring( lastDotPosition + 1, fullClassName.length() );

        if ( lastDotPosition < 0 )
        {
            /* no package name */
            packageName = "";
        }
        else
        {
            packageName = fullClassName.substring( 0, lastDotPosition );
        }
    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setPackageName( String packageName )
    {
        this.packageName = packageName;
    }

    public float getTimeElapsed()
    {
        return this.timeElapsed;
    }

    public void setTimeElapsed( float timeElapsed )
    {
        this.timeElapsed = timeElapsed;
    }

    public void setTestCases( List<ReportTestCase> testCases )
    {
        this.testCases = testCases;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return fullClassName + " [" + getNumberOfTests() + "/" + getNumberOfFailures() + "/" + getNumberOfErrors() + "/"
            + getNumberOfSkipped() + "]";
    }
}
