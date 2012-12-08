package org.apache.maven.surefire.suite;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.maven.shared.utils.xml.PrettyPrintXMLWriter;
import org.apache.maven.shared.utils.xml.Xpp3DomWriter;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class RunResultTest
    extends TestCase
{

    public void testEmptySummaryShouldBeErrorFree()
    {
        RunResult summary = RunResult.noTestsRun();
        assertTrue( summary.isErrorFree() );
    }

    public void testFailuresInFirstRun()
    {
        RunResult resultOne = new RunResult( 10, 1, 3, 2 );
        RunResult resultTwo = new RunResult( 20, 0, 0, 0 );
        assertFalse( resultOne.aggregate( resultTwo ).isErrorFree() );
    }


    public void testAggregatedValues()
    {
        RunResult simple = getSimpleAggregate();
        assertEquals( 20, simple.getCompletedCount() );
        assertEquals( 3, simple.getErrors() );
        assertEquals( 7, simple.getFailures() );
        assertEquals( 4, simple.getSkipped() );

    }

    public void testSerialization()
        throws FileNotFoundException
    {
        writeReadCheck( getSimpleAggregate() );
    }

    public void testFailures()
        throws FileNotFoundException
    {
        writeReadCheck( new RunResult( 0, 1, 2, 3, "stacktraceHere", false ) );
    }

    public void testSkipped()
        throws FileNotFoundException
    {
        writeReadCheck( new RunResult( 3, 2, 1, 0, null, true ) );
    }

    public void testAppendSerialization()
        throws IOException
    {
        RunResult simpleAggregate = getSimpleAggregate();
        RunResult additional = new RunResult( 2, 1, 2, 2, null, true );
        File summary = File.createTempFile( "failsafe", "test" );
        simpleAggregate.writeSummary( summary, false, "utf-8" );
        additional.writeSummary( summary, true, "utf-8" );

        RunResult actual = RunResult.fromInputStream( new FileInputStream( summary ), "utf-8" );
        RunResult expected = simpleAggregate.aggregate( additional );
        assertEquals( expected, actual );
        //noinspection ResultOfMethodCallIgnored
        summary.delete();

    }

    private void writeReadCheck( RunResult simpleAggregate )
        throws FileNotFoundException
    {
        StringWriter writer = getStringWriter( simpleAggregate );

        RunResult actual =
            RunResult.fromInputStream( new ByteArrayInputStream( writer.getBuffer().toString().getBytes() ), "UTF-8" );
        assertEquals( simpleAggregate, actual );
    }

    private StringWriter getStringWriter( RunResult simpleAggregate )
    {
        StringWriter writer = new StringWriter();
        PrettyPrintXMLWriter wr = new PrettyPrintXMLWriter( writer );
        Xpp3DomWriter.write( wr, simpleAggregate.asXpp3Dom() );
        return writer;
    }

    private RunResult getSimpleAggregate()
    {
        RunResult resultOne = new RunResult( 10, 1, 3, 2 );
        RunResult resultTwo = new RunResult( 10, 2, 4, 2 );
        return resultOne.aggregate( resultTwo );
    }
}
