package org.apache.maven.surefire.its;

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

import java.util.ArrayList;

import static org.junit.runners.Parameterized.Parameters;

/**
 * Test class for SUREFIRE-580, configuration parameter <em>skipAfterFailureCount</em>.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class FailFastTestNgIT
    extends AbstractFailFastIT
{

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data()
    {
        ArrayList<Object[]> args = new ArrayList<Object[]>();
        //                        description
        //                                             profile
        //                                                       forkCount,
        //                                                       fail-fast-count
        //                                                                        total
        //                                                                             failures
        //                                                                                     errors
        //                                                                                           skipped
        args.add( new Object[] { "testng-oneFork-ff1", null,     props( 1, 1 ),   5,   1,      0,    4 } );
        args.add( new Object[] { "testng-oneFork-ff2", null,     props( 1, 2 ),   5,   2,      0,    3 } );
        args.add( new Object[] { "testng-twoForks-ff1", null,    props( 2, 1 ),   5,   1,      0,    4 } );
        args.add( new Object[] { "testng-twoForks-ff2", null,    props( 2, 2 ),   5,   2,      0,    3 } );
        args.add( new Object[] { "testng-oneFork-ff3", null,     props( 2, 3 ),   5,   2,      0,    0 } );
        args.add( new Object[] { "testng-twoForks-ff3", null,    props( 2, 3 ),   5,   2,      0,    0 } );
        return args;
    }

    @Override
    protected String withProvider()
    {
        return "testng";
    }
}
