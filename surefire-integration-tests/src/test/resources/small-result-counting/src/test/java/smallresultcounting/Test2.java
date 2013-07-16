package smallresultcounting;

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

import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.fail;

/**
 * @author Kristian Rosenvold
 */
public class Test2
{
    @Test
    public void testiWithFail1()
    {
        fail( "We excpect this1" );
    }

    @Test
    public void testWithException1()
    {
        System.out.println( "testWithException1 to stdout" );
        System.err.println( "testWithException1 to stderr" );
        throw new RuntimeException( "We expect this1-1" );
    }

    @Test
    public void testWithException2()
    {
        throw new RuntimeException( "We expect this1-2" );
    }


    @Ignore( "We do this for a reason1" )
    @Test
    public void testWithIgnore1()
    {
    }

    @Ignore( "We do this for a reason2" )
    @Test
    public void testWithIgnore2()
    {
    }

    @Ignore
    @Test
    public void testWithIgnore3()
    {
    }

    @Test
    public void testAllok1()
    {
        System.out.println( "testAllok1 to stdout" );
        System.err.println( "testAllok1 to stderr" );
        try
        {
            Thread.sleep( 100 );
        }
        catch ( InterruptedException ignore )
        {
        }
    }

    @Test
    public void testAllok2()
    {
    }

    @Test
    public void testAllok3()
    {
        try
        {
            Thread.sleep( 250 );
        }
        catch ( InterruptedException ignore )
        {
        }
    }

    @Test
    public void testAllok4()
    {
    }

}