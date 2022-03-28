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

import org.junit.runners.Parameterized.Parameters;

/**
 *
 */
public class FailFastJUnit5IT
    extends AbstractFailFastIT
{

    @Parameters( name = "{0}" )
    @SuppressWarnings( "checkstyle:linelength" )
    public static Iterable<Object[]> data()
    {
        /*
         * reuseForks=false is not used because of race conditions between forks.
         */
        ArrayList<Object[]> args = new ArrayList<>();
        //                        description
        //                                             profile
        //                                                       forkCount,
        //                                                       fail-fast-count,
        //                                                       reuseForks
        //                                                                               total
        //                                                                                    failures
        //                                                                                            errors
        //                                                                                                  skipped
        //                                                                                                        pipes
        args.add( new Object[] { "fc1", null,     props( 1, 3, true ),    5,   0,      3,    0, true } );
        args.add( new Object[] { "fc2", null,     props( 2, 3, true ),    5,   0,      3,    0, true } );
        return args;
    }

    @Override
    protected String withProvider()
    {
        return "junit5";
    }
}
