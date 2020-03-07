package org.apache.maven.surefire.its.fixture;

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

/**
 *
 */
public class IntegrationTestSuiteResults
{
    private int total, errors, failures, skipped, flakes;

    public IntegrationTestSuiteResults( int total, int errors, int failures, int skipped )
    {
        this.total = total;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
    }

    public IntegrationTestSuiteResults( int total, int errors, int failures, int skipped, int flakes )
    {
        this( total, errors, failures, skipped );
        this.flakes = flakes;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal( int total )
    {
        this.total = total;
    }

    public int getErrors()
    {
        return errors;
    }

    public void setErrors( int errors )
    {
        this.errors = errors;
    }

    public int getFailures()
    {
        return failures;
    }

    public void setFailures( int failures )
    {
        this.failures = failures;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public void setSkipped( int skipped )
    {
        this.skipped = skipped;
    }

    public int getFlakes()
    {
        return flakes;
    }

    public void setFlakes( int flakes )
    {
        this.flakes = flakes;
    }

}
