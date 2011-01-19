package org.apache.maven.surefire.booter;

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
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class PropertiesWrapperTest
    extends TestCase
{
    public void testAddList()
        throws Exception
    {

        Properties props = new Properties();
        PropertiesWrapper propertiesWrapper = new PropertiesWrapper( props );
        List items = new ArrayList();
        items.add( "String1" );
        items.add( "String2,String3" );
        items.add( "String4" );
        items.add( "String5," );
        propertiesWrapper.addList( items, "Test" );

        final List test = propertiesWrapper.getStringList( "Test" );
        assertEquals( 5, test.size() );
        assertEquals( "String5", test.get( 4 ) );
        assertEquals( "String3", test.get( 2 ) );
        assertEquals( "String2", test.get( 1 ) );

    }
}
