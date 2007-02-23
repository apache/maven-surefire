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

import org.apache.maven.surefire.report.ReportEntry;

/**
 * Surefire output consumer that will be called from Surefire when forking tests to process
 * the lines of the surefire header, messages, footer and test output from the forked Surefire execution.
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @since 2.1
 */
public interface OutputConsumer
{

    /**
     * Process a line from Surefire header
     *
     * @param line the line to process
     */
    void consumeHeaderLine( String line );

    /**
     * Process a line with a Surefire message (not part of test output)
     *
     * @param line the line to process
     */
    void consumeMessageLine( String line );

    /**
     * Process a line from Surefire footer
     *
     * @param line the line to process
     */
    void consumeFooterLine( String line );

    /**
     * Process a line from test output
     *
     * @param line the line to process
     */
    void consumeOutputLine( String line );

    /**
     * This method will be called when a test set starts, before consuming surefire message lines.
     *
     * @param reportEntry The {@link ReportEntry} with the name and group (optional) of the test that starts
     */
    void testSetStarting( ReportEntry reportEntry );

    /**
     * This method will be called when a test set ends, after consuming all its surefire message lines.
     */
    void testSetCompleted();

}
