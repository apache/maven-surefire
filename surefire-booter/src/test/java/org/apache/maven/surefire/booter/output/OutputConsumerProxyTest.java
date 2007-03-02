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

import org.jmock.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;

/**
 * Test for {@link OutputConsumerProxy}
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class OutputConsumerProxyTest
    extends AbstractOutputConsumerTestCase
{
    private Mock outputConsumerMock;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        setOutputConsumerMock( new Mock( OutputConsumer.class ) );
        setOutputConsumer( new OutputConsumerProxy( (OutputConsumer) getOutputConsumerMock().proxy() ) );
    }

    public void setOutputConsumerMock( Mock outputConsumerMock )
    {
        this.outputConsumerMock = outputConsumerMock;
    }

    public Mock getOutputConsumerMock()
    {
        return outputConsumerMock;
    }

    public void testConsumeFooterLine()
    {
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "consumeFooterLine" )
            .with( new IsEqual( getLine() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetCompleted" );
        super.testConsumeFooterLine();
        getOutputConsumerMock().verify();
    }

    public void testConsumeHeaderLine()
    {
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "consumeHeaderLine" )
            .with( new IsEqual( getLine() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetCompleted" );
        super.testConsumeHeaderLine();
        getOutputConsumerMock().verify();
    }

    public void testConsumeMessageLine()
    {
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "consumeMessageLine" )
            .with( new IsEqual( getLine() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetCompleted" );
        super.testConsumeMessageLine();
        getOutputConsumerMock().verify();
    }

    public void testConsumeOutputLine()
        throws Exception
    {
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "consumeOutputLine" )
            .with( new IsEqual( getLine() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetCompleted" );
        super.testConsumeOutputLine();
        getOutputConsumerMock().verify();
    }

    public void testTestSetStarting()
    {
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        super.testTestSetStarting();
        getOutputConsumerMock().verify();
    }

    public void testTestSetCompleted()
    {
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetCompleted" );
        super.testTestSetCompleted();
        getOutputConsumerMock().verify();
    }

}
