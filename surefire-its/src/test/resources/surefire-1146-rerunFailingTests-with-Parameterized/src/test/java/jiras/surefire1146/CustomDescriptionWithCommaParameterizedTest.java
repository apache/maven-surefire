package jiras.surefire1146;

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
import java.util.List;

import junit.runner.Version;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

@RunWith( Parameterized.class )
public class CustomDescriptionWithCommaParameterizedTest
{

    private static boolean success;

    public CustomDescriptionWithCommaParameterizedTest( String test1, String test2, String test3 )
    {

    }

    @Parameters( name = "{index}: ({0}), {1}, {2};" )
    public static List getParameters()
    {
        List parameters = new ArrayList();
        parameters.add( new String[]{ "Test11", "Test12", "Test13" } );
        parameters.add( new String[]{ "Test21", "Test22", "Test23" } );
        parameters.add( new String[]{ "Test31", "Test32", "Test33" } );
        return parameters;
    }

    @Test
    public void flakyTest()
    {
        System.out.println( "Running JUnit " + Version.id() );
        boolean current = success;
        success = !success;
        assertTrue( current );
    }

}
