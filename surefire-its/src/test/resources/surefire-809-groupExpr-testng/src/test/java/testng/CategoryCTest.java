package testng;
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class CategoryCTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;

    @Test( groups = "CategoryA" )
    public void testInCategoryA()
    {
        catACount++;
    }

    @Test( groups = "CategoryB" )
    public void testInCategoryB()
    {
        catBCount++;
    }

    @Test( groups = { "CategoryA", "CategoryB" } )
    public void testInCategoriesAB()
    {
        System.out.println( getClass().getSimpleName() + ".testInCategoriesAB()" );
        catACount++;
        catBCount++;
    }

    @Test( groups="CategoryC" )
    public void testInCategoryC()
    {
        catCCount++;
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        System.out.println("mA: " + catACount + "\n" +
            "mB: " + catBCount + "\n" +
            "mC: " + catCCount );
    }
}
