package sample.parameterized;

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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import sample.CategoryActivated;

@RunWith( Parameterized.class )
public class Parameterized02Test
{
    static
    {
        System.out.println( "Initializing Parameterized02Test" );
    }

    @Parameters
    public static Collection<Integer[]> getParams()
    {
        return Arrays.asList( new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }, new Integer[] { 4 } );
    }

    public Parameterized02Test( Integer param )
    {
    }

    @Test
    @Category( CategoryActivated.class )
    public void testNothing()
    {
    }

    @Test
    public void testNothingEither()
    {
    }
}
