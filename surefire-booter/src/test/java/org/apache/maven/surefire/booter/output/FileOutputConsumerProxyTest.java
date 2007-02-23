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

import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Test for {@link FileOutputConsumerProxy}
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class FileOutputConsumerProxyTest
    extends OutputConsumerProxyTest
{

    private static final String USER_DIR = System.getProperty( "user.dir" );

    protected void setUp()
        throws Exception
    {
        super.setUp();
        setOutputConsumer( new FileOutputConsumerProxy( (OutputConsumer) getOutputConsumerMock().proxy() ) );
    }

    public void testConsumeOutputLine()
        throws Exception
    {
        File reportFile = new File( USER_DIR, getReportEntry().getName() + "-output.txt" );
        reportFile.delete();

        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetStarting" )
            .with( new IsEqual( getReportEntry() ) );
        getOutputConsumerMock().expects( new InvokeOnceMatcher() ).method( "testSetCompleted" );
        getOutputConsumer().testSetStarting( getReportEntry() );
        getOutputConsumer().consumeOutputLine( getLine() );
        getOutputConsumer().testSetCompleted();
        getOutputConsumerMock().verify();

        assertTrue( reportFile.exists() );

        BufferedReader in = null;
        try
        {
            in = new BufferedReader( new InputStreamReader( new FileInputStream( reportFile ) ) );
            String content = in.readLine();
            assertEquals( getLine(), content );
            assertNull( in.readLine() );
        }
        finally
        {
            if ( in != null )
            {
                in.close();
            }
        }

        reportFile.delete();
    }

}
