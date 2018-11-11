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
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;
import org.apache.maven.surefire.booter.KeyValueSource;

import static java.util.Collections.list;

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
        SurefireProperties orderedProperties = new SurefireProperties( (KeyValueSource) null );
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
        // keys() uses the items property, more reliable to test than size(),
        // which is based on the Properties class
        // see https://issues.apache.org/jira/browse/SUREFIRE-1445
        assertEquals( src.size(), list( orderedProperties.keys() ).size() );
        assertEquals( src.size(), size( orderedProperties.getStringKeySet().iterator() ) );
        assertEquals( 2, orderedProperties.size() );

        assertTrue( list( orderedProperties.keys() ).contains( "a" ) );
        assertTrue( list( orderedProperties.keys() ).contains( "b" ) );

        Iterator it = orderedProperties.getStringKeySet().iterator();
        SortedSet<Object> keys = new TreeSet<>();
        keys.add( it.next() );
        keys.add( it.next() );
        it = keys.iterator();
        assertEquals( "a", it.next() );
        assertEquals( "b", it.next() );
    }

    public void testPutAll()
    {
        Properties src = new Properties();
        src.setProperty( "a", "1" );
        src.setProperty( "b", "2" );
        SurefireProperties orderedProperties = new SurefireProperties();
        orderedProperties.putAll( src );
        // Cannot make assumptions about insertion order
        // keys() uses the items property, more reliable to test than size(),
        // which is based on the Properties class
        // see https://issues.apache.org/jira/browse/SUREFIRE-1445
        assertEquals( src.size(), list( orderedProperties.keys() ).size() );
        assertEquals( src.size(), size( orderedProperties.getStringKeySet().iterator() ) );
        assertEquals( 2, orderedProperties.size() );

        assertTrue( list( orderedProperties.keys() ).contains( "a" ) );
        assertTrue( list( orderedProperties.keys() ).contains( "b" ) );

        Iterator it = orderedProperties.getStringKeySet().iterator();
        SortedSet<Object> keys = new TreeSet<>();
        keys.add( it.next() );
        keys.add( it.next() );
        it = keys.iterator();
        assertEquals( "a", it.next() );
        assertEquals( "b", it.next() );
    }

    private static int size( Iterator<?> iterator )
    {
        int count = 0;
        while ( iterator.hasNext() ) {
            iterator.next();
            count++;
        }
        return count;
    }

}
