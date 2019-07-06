package consoleOutput;

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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.fail;

public class Test1
{
    public Test1()
    {
       System.out.println("In constructor");
    }

    @Before
    public void t1()
    {
        System.out.println( "t1 = " + System.currentTimeMillis() );
    }

    @After
    public void t2()
    {
        System.out.println( "t2 = " + System.currentTimeMillis() );
    }

    @Test
    public void testStdOut()
    {
        char c = 'C';
        System.out.print( "Sout" );
        System.out.print( "Again" );
        System.out.print( "\n" );
        System.out.print( c );
        System.out.println( "SoutLine" );
        System.out.println( "äöüß" );
        System.out.println( "" );
        System.out.println( "==END==" );

        fail( "failing with ü" );
    }
        
    @Test
    public void testStdErr()
    {
        char e = 'E';
        System.err.print( "Serr" );
        System.err.print( "\n" );
        System.err.print( e );
        System.err.println( "SerrLine" );
        System.err.println( "äöüß" );
        System.err.println( "" );
        System.err.println( "==END==" );
    }
}
