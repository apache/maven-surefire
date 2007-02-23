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

import java.io.StringWriter;

/**
 * Test for {@link PrintWriterOutputConsumer}
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class PrintWriterOutputConsumerTest
    extends AbstractOutputConsumerTest
{
    private StringWriter writer;

    private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    protected void setUp()
        throws Exception
    {
        super.setUp();
        writer = new StringWriter();
        setOutputConsumer( new PrintWriterOutputConsumer( writer ) );
    }

    public void testConsumeFooterLine()
    {
        super.testConsumeFooterLine();
        assertEquals( this.getLine() + LINE_SEPARATOR, writer.toString() );
    }

    public void testConsumeHeaderLine()
    {
        super.testConsumeHeaderLine();
        assertEquals( getLine() + LINE_SEPARATOR, writer.toString() );
    }

    public void testConsumeMessageLine()
    {
        super.testConsumeMessageLine();
        assertEquals( getLine() + LINE_SEPARATOR, writer.toString() );
    }

    public void testConsumeOutputLine()
        throws Exception
    {
        super.testConsumeOutputLine();
        assertEquals( getLine() + LINE_SEPARATOR, writer.toString() );
    }

    public void testTestSetCompleted()
    {
        super.testTestSetCompleted();
        assertEquals( "", writer.toString() );
    }

    public void testTestSetStarting()
    {
        super.testTestSetStarting();
        assertEquals( "", writer.toString() );
    }

}
