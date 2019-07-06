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
 * Test class for SUREFIRE-580, configuration parameter {@code skipAfterFailureCount}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class FailFastJUnitIT
    extends AbstractFailFastIT
{
    @Parameters( name = "{0}" )
    @SuppressWarnings( { "checkstyle:visibilitymodifier", "checkstyle:linelength" } )
    public static Iterable<Object[]> data()
    {
        /**
         * reuseForks=false is not used because of race conditions and unpredictable commands received by
         * MasterProcessReader, this feature has significant limitation.
         */
        ArrayList<Object[]> args = new ArrayList<>();
        //                        description
        //                                             profile
        //                                                         forkCount,
        //                                                         fail-fast-count,
        //                                                         reuseForks
        //                                                                               total
        //                                                                                    failures
        //                                                                                            errors
        //                                                                                                  skipped
        //                                                                                                        pipes
        args.add( new Object[] { "junit4-oneFork-ff1", "junit4",   props( 1, 1, true ),  5,   0,      1,    4, true } );
        args.add( new Object[] { "junit47-oneFork-ff1", "junit47", props( 1, 1, true ),  5,   0,      1,    4, true } );
        args.add( new Object[] { "junit4-oneFork-ff2", "junit4",   props( 1, 2, true ),  5,   0,      2,    3, true } );
        args.add( new Object[] { "junit47-oneFork-ff2", "junit47", props( 1, 2, true ),  5,   0,      2,    3, true } );
        args.add( new Object[] { "junit4-twoForks-ff1", "junit4",  props( 2, 1, true ),  5,   0,      2,    3, true } );
        args.add( new Object[] { "junit47-twoForks-ff1", "junit47", props( 2, 1, true ),  5,   0,      2,    3, true } );
        args.add( new Object[] { "junit4-twoForks-ff2", "junit4",  props( 2, 2, true ),  5,   0,      2,    2, true } );
        args.add( new Object[] { "junit4-twoForks-ff2-tcp", "junit4",  props( 2, 2, true ),  5,   0,      2,    2, false } );
        args.add( new Object[] { "junit47-twoForks-ff2", "junit47", props( 2, 2, true ),  5,   0,      2,    2, true } );
        args.add( new Object[] { "junit4-oneFork-ff3", "junit4",   props( 1, 3, true ),  5,   0,      2,    0, true } );
        args.add( new Object[] { "junit47-oneFork-ff3", "junit47", props( 1, 3, true ),  5,   0,      2,    0, true } );
        args.add( new Object[] { "junit4-twoForks-ff3", "junit4",  props( 2, 3, true ),  5,   0,      2,    0, true } );
        args.add( new Object[] { "junit47-twoForks-ff3", "junit47", props( 2, 3, true ),  5,   0,      2,    0, true } );
        args.add( new Object[] { "junit47-twoForks-ff3-tcp", "junit47", props( 2, 3, true ),  5,   0,      2,    0, false } );
        /*args.add( new Object[] { "junit4-twoForks-ff1x","junit4",  props( 2, 1, false ), 5,   0,      2,    3 } );
        args.add( new Object[] { "junit47-twoForks-ff1x","junit47",props( 2, 1, false ), 5,   0,      2,    3 } );
        args.add( new Object[] { "junit4-twoForks-ff2x","junit4",  props( 2, 2, false ), 5,   0,      2,    2 } );
        args.add( new Object[] { "junit47-twoForks-ff2x","junit47",props( 2, 2, false ), 5,   0,      2,    2 } );*/
        return args;
    }

    @Override
    protected String withProvider()
    {
        return "junit";
    }
}
