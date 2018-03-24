package forkConsoleOutput;

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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class Test1
{
    @Test
    public void test6281() {
        System.out.println( "Test1 on" + Thread.currentThread().getName());
    }

    @Test
    public void nullPointerInLibrary() {
        new File((String)null);
    }

    @Test
    public void failInMethod() {
        innerFailure();
    }

    @Test
    public void failInLibInMethod() {
        new File((String)null);
    }


    @Test
    public void failInNestedLibInMethod() {
        nestedLibFailure();
    }

    @Test
    public void assertion1() {
        Assert.assertEquals("Bending maths", "123", "312");
    }

    @Test
    public void assertion2() {
        Assert.assertFalse("True is false", true);
    }

    private void innerFailure(){
        throw new NullPointerException("Fail here");
    }

    private void nestedLibFailure(){
        new File((String) null);
    }

    @BeforeClass
    public static void testWithFailingAssumption2() {
        System.out.println( "BeforeTest1 on" + Thread.currentThread().getName());
    }
    
    @AfterClass
    public static void testWithFailingAssumption3() {
        System.out.println( "AfterTest1 on" + Thread.currentThread().getName());
    }

}
