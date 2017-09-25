package jiras.surefire1082;

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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith( Parameterized.class )
public class Jira1082Test
{
    private final int x;

    public Jira1082Test( int x )
    {
        this.x = x;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data()
    {
        return Arrays.asList( new Object[][]{ { 0 }, { 1 } } );
    }

    @Test
    public void a()
        throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep( 500 );
        System.out.println( getClass() + " a " + x + " " + Thread.currentThread().getName() );
    }

    @Test
    public void b()
        throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep( 500 );
        System.out.println( getClass() + " b " + x + " " + Thread.currentThread().getName() );
    }
}
