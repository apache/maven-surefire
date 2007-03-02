package org.apache.maven.surefire.booter.output;

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

import junit.framework.TestCase;
import org.apache.maven.surefire.report.ReportEntry;

/**
 * Test for {@link OutputConsumer}
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public abstract class AbstractOutputConsumerTestCase
    extends TestCase
{
    private OutputConsumer outputConsumer;

    private String line;

    private ReportEntry reportEntry;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        setLine( "line" );
        setReportEntry( new ReportEntry() );
        getReportEntry().setGroup( "group" );
        getReportEntry().setName( "name" );
    }

    public void setOutputConsumer( OutputConsumer outputConsumer )
    {
        this.outputConsumer = outputConsumer;
    }

    public OutputConsumer getOutputConsumer()
    {
        return outputConsumer;
    }

    public void setLine( String line )
    {
        this.line = line;
    }

    public String getLine()
    {
        return line;
    }

    public void setReportEntry( ReportEntry reportEntry )
    {
        this.reportEntry = reportEntry;
    }

    public ReportEntry getReportEntry()
    {
        return reportEntry;
    }

    public void testConsumeHeaderLine()
    {
        getOutputConsumer().testSetStarting( getReportEntry() );
        getOutputConsumer().consumeHeaderLine( getLine() );
        getOutputConsumer().testSetCompleted();
    }

    public void testConsumeMessageLine()
    {
        getOutputConsumer().testSetStarting( getReportEntry() );
        getOutputConsumer().consumeMessageLine( getLine() );
        getOutputConsumer().testSetCompleted();
    }

    public void testConsumeFooterLine()
    {
        getOutputConsumer().testSetStarting( getReportEntry() );
        getOutputConsumer().consumeFooterLine( getLine() );
        getOutputConsumer().testSetCompleted();
    }

    public void testConsumeOutputLine()
        throws Exception
    {
        getOutputConsumer().testSetStarting( getReportEntry() );
        getOutputConsumer().consumeOutputLine( getLine() );
        getOutputConsumer().testSetCompleted();
    }

    public void testTestSetStarting()
    {
        getOutputConsumer().testSetStarting( getReportEntry() );
    }

    public void testTestSetCompleted()
    {
        getOutputConsumer().testSetStarting( getReportEntry() );
        getOutputConsumer().testSetCompleted();
    }

}
