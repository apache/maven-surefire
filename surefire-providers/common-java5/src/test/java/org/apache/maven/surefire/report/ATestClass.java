package org.apache.maven.surefire.report;

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

import java.io.File;

/**
 *
 */
@SuppressWarnings( "UnusedDeclaration" )
public class ATestClass
{

    public void failInAssert()
    {
        throw new AssertionError( "X is not Z" );
    }

    public void nestedFailInAssert()
    {
        failInAssert();
    }

    public void npe()
    {
        throw new NullPointerException( "It was null" );
    }

    public void nestedNpe()
    {
        npe();
    }

    public void npeOutsideTest()
    {
        File file = new File( (String) null );
    }

    public void nestedNpeOutsideTest()
    {
        npeOutsideTest();
    }

    public void aLongTestErrorMessage()
    {
        throw new RuntimeException( "This message won't be truncated, somewhere over the rainbow. "
                                    + "Gangnam style, Gangnam style, Gangnam style, , Gangnam style, Gangnam style" );
    }

    public void aMockedException()
    {
        throw new SomeMockedException();
    }
}
