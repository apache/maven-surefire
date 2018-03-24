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

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    
    static
    {
        System.out.println( "Loading " + AppTest.class.getName() );
        
        Properties p = System.getProperties();
        p.setProperty( "Foo", "Bar" );
        System.setProperties( p );
    }
    
    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        System.out.println( "Expecting: Bar\nGot: " + System.getProperty( "Foo" ) );
        assertEquals( "Expecting: Bar\nGot: " + System.getProperty( "Foo" ), "Bar", System.getProperty( "Foo" ) );
    }
}
