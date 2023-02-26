package org.apache.maven.surefire.api.util.internal;

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
 * Utility class for {@link org.apache.maven.surefire.api.report.ReportEntry}.
 */
public final class ReportEntryUtils
{
    private ReportEntryUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    /**
     * @param sourceId class id or source id (parent)
     * @param testId   test child
     * @return shifts {@code sourceId} to 32-bit MSB of long and encodes {@code testId} to LSB
     */
    public static long toTestRunId( int sourceId, int testId )
    {
        return toTestRunId( sourceId ) | testId;
    }

    /**
     * @param sourceId class id or source id (parent)
     * @return shifts in 32 bits
     */
    public static long toTestRunId( int sourceId )
    {
        return ( (long) sourceId ) << 32;
    }

    /**
     * @param testRunId encoded 32-bit MSB source and 32-bit LSB name in 64-bit value
     * @return shifts {@code testRunId} in 32 bits right
     */
    public static int toSourceId( long testRunId )
    {
        return (int) ( 0x00000000ffffffffL & ( testRunId >>> 32 ) );
    }

    public static boolean existsSourceId( Long testRunId )
    {
        return testRunId != null && toSourceId( testRunId ) != 0;
    }

    /**
     *
     * @param testRunId encoded 32-bit MSB source and 32-bit LSB name in 64-bit value
     * @return 32-bit LSB of {@code testRunId}
     */
    public static int toNameId( long testRunId )
    {
        return (int) ( 0x00000000ffffffffL & testRunId );
    }

    public static boolean existsNameId( Long testRunId )
    {
        return testRunId != null && toNameId( testRunId ) != 0;
    }
}
