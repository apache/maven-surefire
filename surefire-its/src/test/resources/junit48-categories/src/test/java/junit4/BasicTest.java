package junit4;
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
import org.junit.Test;
import org.junit.experimental.categories.Category;


public class BasicTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;
    static int catNoneCount = 0;

    @Test
    @Category(CategoryA.class)
    public void testInCategoryA()
    {
        System.out.println( "Ran testInCategoryA" );
        catACount++;
    }

    @Test
    @Category(CategoryB.class)
    public void testInCategoryB()
    {
        System.out.println( "Ran testInCategoryB" );
        catBCount++;
    }

    @Test
    @Category(CategoryC.class)
    public void testInCategoryC()
    {
        System.out.println( "Ran testInCategoryC" );
        catCCount++;
    }

    @Test
    public void testInNoCategory()
    {
        System.out.println( "Ran testInNoCategory" );
        catNoneCount++;
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        System.out.println("catA: " + catACount + "\n" +
            "catB: " + catBCount + "\n" +
            "catC: " + catCCount + "\n" +
            "catNone: " + catNoneCount);
    }
}
