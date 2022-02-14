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

import static org.apache.maven.shared.utils.StringUtils.isNotBlank;

/**
 *
 */
public final class ReportTestCase
{
    private String fullClassName;

    private String className;

    private String fullName;

    private String name;

    private float time;

    private String failureMessage;

    private String failureType;

    private String failureErrorLine;

    private String failureDetail;

    private boolean hasFailure;

    private boolean hasError;

    private boolean hasSkipped;

    public String getName()
    {
        return name;
    }

    public ReportTestCase setName( String name )
    {
        this.name = name;
        return this;
    }

    public String getFullClassName()
    {
        return fullClassName;
    }

    public ReportTestCase setFullClassName( String name )
    {
        fullClassName = name;
        return this;
    }

    public String getClassName()
    {
        return className;
    }

    public ReportTestCase setClassName( String name )
    {
        className = name;
        return this;
    }

    public float getTime()
    {
        return time;
    }

    public ReportTestCase setTime( float time )
    {
        this.time = time;
        return this;
    }

    public String getFullName()
    {
        return fullName;
    }

    public ReportTestCase setFullName( String fullName )
    {
        this.fullName = fullName;
        return this;
    }

    public String getFailureMessage()
    {
        return failureMessage;
    }

    private ReportTestCase setFailureMessage( String failureMessage )
    {
        this.failureMessage = failureMessage;
        return this;
    }

    public String getFailureType()
    {
        return failureType;
    }

    public String getFailureErrorLine()
    {
        return failureErrorLine;
    }

    public ReportTestCase setFailureErrorLine( String failureErrorLine )
    {
        this.failureErrorLine = failureErrorLine;
        return this;
    }

    public String getFailureDetail()
    {
        return failureDetail;
    }

    public ReportTestCase setFailureDetail( String failureDetail )
    {
        this.failureDetail = failureDetail;
        return this;
    }

    public ReportTestCase setFailure( String message, String type )
    {
        hasFailure = isNotBlank( type );
        hasError = false;
        hasSkipped = false;
        return setFailureMessage( message ).setFailureType( type );
    }

    public ReportTestCase setError( String message, String type )
    {
        hasFailure = false;
        hasError = isNotBlank( type );
        hasSkipped = false;
        return setFailureMessage( message ).setFailureType( type );
    }

    public ReportTestCase setSkipped( String message )
    {
        hasFailure = false;
        hasError = false;
        hasSkipped = isNotBlank( message );
        return setFailureMessage( message ).setFailureType( "skipped" );
    }

    public boolean isSuccessful()
    {
        return !hasFailure() && !hasError() && !hasSkipped();
    }

    public boolean hasFailure()
    {
        return hasFailure;
    }

    public boolean hasError()
    {
        return hasError;
    }

    public boolean hasSkipped()
    {
        return hasSkipped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return fullName;
    }

    private ReportTestCase setFailureType( String failureType )
    {
        this.failureType = failureType;
        return this;
    }
}
