package org.apache.maven.plugin.surefire.report;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This test ensures a test with a huge number of test methods (e.g. parameterized test)
 * does not breach the system's file handle limits.
 *
 * @author Markus Spann
 */
@RunWith( Parameterized.class )
public class HugeParameterizedTestPrintingToStdout extends TestCase
{

    private static final int NB_TESTS = 2500;

    private final int        num;

    public HugeParameterizedTestPrintingToStdout( int num )
    {
        this.num = num;
    }

    @Test
    public void printLineToStdout()
    {
        System.out.printf( "Test #%d / %d%n", num, NB_TESTS );
    }

    @Parameters
    public static Iterable<Object[]> getIntRange()
    {
        List<Object[]> collect = IntStream.rangeClosed( 1, NB_TESTS )
                .mapToObj( Integer::valueOf )
                .map( i -> new Integer[] {i} )
                .collect( Collectors.toList() );
        return collect;
    }

}
