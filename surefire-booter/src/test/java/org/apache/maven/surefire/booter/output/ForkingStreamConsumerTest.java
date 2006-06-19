package org.apache.maven.surefire.booter.output;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.apache.maven.surefire.report.ReportEntry;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Test for {@link ForkingStreamConsumer}
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class ForkingStreamConsumerTest
    extends MockObjectTestCase
{

    private ForkingStreamConsumer streamConsumer;

    private Mock outputConsumerMock;

    private String message;

    private ReportEntry reportEntry;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        outputConsumerMock = new Mock( OutputConsumer.class );
        streamConsumer = new ForkingStreamConsumer( (OutputConsumer) outputConsumerMock.proxy() );
        message = "message";
        reportEntry = new ReportEntry();
        reportEntry.setGroup( "group" );
        reportEntry.setName( "name" );
    }

    public void testConsumeHeaderLine()
    {
        String message = "message";
        String line = ForkingConsoleReporter.FORKING_PREFIX_HEADING + message;
        outputConsumerMock.expects( once() ).method( "consumeHeaderLine" ).with( eq( message ) );
        streamConsumer.consumeLine( line );
    }

    public void testConsumeMessageLine()
    {
        String message = "message";
        String line = ForkingConsoleReporter.FORKING_PREFIX_STANDARD + message;
        outputConsumerMock.expects( once() ).method( "consumeMessageLine" ).with( eq( message ) );
        streamConsumer.consumeLine( line );
    }

    public void testConsumeFooterLine()
    {
        String message = "message";
        String line = ForkingConsoleReporter.FORKING_PREFIX_FOOTER + message;
        outputConsumerMock.expects( once() ).method( "consumeFooterLine" ).with( eq( message ) );
        streamConsumer.consumeLine( line );
    }

    public void testConsumeOutputLine()
    {
        String line = message;
        outputConsumerMock.expects( once() ).method( "consumeOutputLine" ).with( eq( message ) );
        streamConsumer.consumeLine( line );
    }

    public void testTestSetStarting()
    {
        message = ForkingConsoleReporter.getTestSetStartingMessage( reportEntry );
        String line = ForkingConsoleReporter.FORKING_PREFIX_STANDARD + message;
        outputConsumerMock.expects( once() ).method( "testSetStarting" ).with( eq( reportEntry ) );
        outputConsumerMock.expects( once() ).method( "consumeMessageLine" ).with( eq( message ) );
        streamConsumer.consumeLine( line );
    }

    public void testTestSetCompleted()
    {
        message = "Tests run: xxxx";
        String line = ForkingConsoleReporter.FORKING_PREFIX_STANDARD + message;
        outputConsumerMock.expects( once() ).method( "testSetCompleted" );
        outputConsumerMock.expects( once() ).method( "consumeMessageLine" ).with( eq( message ) );
        streamConsumer.consumeLine( line );
    }

}
