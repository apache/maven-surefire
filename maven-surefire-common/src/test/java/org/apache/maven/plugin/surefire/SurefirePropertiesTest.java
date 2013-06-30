package org.apache.maven.plugin.surefire;
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

import java.util.Enumeration;
import java.util.Properties;

import junit.framework.TestCase;
import org.apache.maven.surefire.booter.KeyValueSource;

/**
 * Tests the insertion-order preserving properties collection
 */
public class SurefirePropertiesTest
    extends TestCase
{

    public void testKeys()
        throws Exception
    {
        SurefireProperties orderedProperties = new SurefireProperties( (KeyValueSource) null );
        orderedProperties.setProperty( "abc", "1" );
        orderedProperties.setProperty( "xyz", "1" );
        orderedProperties.setProperty( "efg", "1" );

        Enumeration<Object> keys = orderedProperties.keys();
        assertEquals( "abc", keys.nextElement() );
        assertEquals( "xyz", keys.nextElement() );
        assertEquals( "efg", keys.nextElement() );

    }

    public void testKeysReinsert()
        throws Exception
    {
        SurefireProperties orderedProperties = new SurefireProperties( (KeyValueSource)null );
        orderedProperties.setProperty( "abc", "1" );
        orderedProperties.setProperty( "xyz", "1" );
        orderedProperties.setProperty( "efg", "1" );
        orderedProperties.setProperty( "abc", "2" );
        orderedProperties.remove( "xyz" );
        orderedProperties.setProperty( "xyz", "1" );

        Enumeration<Object> keys = orderedProperties.keys();
        assertEquals( "abc", keys.nextElement() );
        assertEquals( "efg", keys.nextElement() );
        assertEquals( "xyz", keys.nextElement() );
    }

    public void testConstructWithOther()
    {
        Properties src = new Properties();
        src.setProperty( "a", "1" );
        src.setProperty( "b", "2" );
        SurefireProperties orderedProperties = new SurefireProperties( src );
        // Cannot make assumptions about insertion order
        assertEquals( 2, orderedProperties.size() );


    }

}
