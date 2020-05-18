package org.apache.maven.surefire.api.util.internal;

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

import org.apache.maven.surefire.api.util.internal.ImmutableMap.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @since 2.20
 */
public class ImmutableMapTest
{
    private ImmutableMap<String, String> map;

    @Before
    public void setUp()
    {
        Map<String, String> backingMap = new LinkedHashMap<>();
        backingMap.put( "a", "1" );
        backingMap.put( "x", null );
        backingMap.put( "b", "2" );
        backingMap.put( "c", "3" );
        backingMap.put( "", "" );
        backingMap.put( null, "1" );
        map = new ImmutableMap<>( backingMap );
    }

    @Test
    public void testEntrySet()
    {
        Set<Entry<String, String>> entries = map.entrySet();
        assertThat( entries, hasSize( 6 ) );
        assertThat( entries, hasItem( new Node<>( "a", "1" ) ) );
        assertThat( entries, hasItem( new Node<>( "x", (String) null ) ) );
        assertThat( entries, hasItem( new Node<>( "b", "2" ) ) );
        assertThat( entries, hasItem( new Node<>( "c", "3" ) ) );
        assertThat( entries, hasItem( new Node<>( "", "" ) ) );
        assertThat( entries, hasItem( new Node<>( (String) null, "1" ) ) );
    }

    @Test
    public void testGetter()
    {
        assertThat( map.size(), is( 6 ) );
        assertThat( map.get( "a" ), is( "1" ) );
        assertThat( map.get( "x" ), is( (String) null ) );
        assertThat( map.get( "b" ), is( "2" ) );
        assertThat( map.get( "c" ), is( "3" ) );
        assertThat( map.get( "" ), is( "" ) );
        assertThat( map.get( null ), is( "1" ) );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotModifyEntries()
    {
        map.entrySet().clear();
    }

    @Test
    public void shouldSafelyEnumerateEntries()
    {
        Iterator<Entry<String, String>> it = map.entrySet().iterator();

        assertThat( it.hasNext(), is( true ) );
        Entry<String, String> val = it.next();
        assertThat( val.getKey(), is( "a" ) );
        assertThat( val.getValue(), is( "1" ) );

        assertThat( it.hasNext(), is( true ) );
        val = it.next();
        assertThat( val.getKey(), is( "x" ) );
        assertThat( val.getValue(), is( nullValue() ) );

        assertThat( it.hasNext(), is( true ) );
        val = it.next();
        assertThat( val.getKey(), is( "b" ) );
        assertThat( val.getValue(), is( "2" ) );

        assertThat( it.hasNext(), is( true ) );
        val = it.next();
        assertThat( val.getKey(), is( "c" ) );
        assertThat( val.getValue(), is( "3" ) );

        assertThat( it.hasNext(), is( true ) );
        val = it.next();
        assertThat( val.getKey(), is( "" ) );
        assertThat( val.getValue(), is( "" ) );

        assertThat( it.hasNext(), is( true ) );
        val = it.next();
        assertThat( val.getKey(), is( nullValue() ) );
        assertThat( val.getValue(), is( "1" ) );

        assertThat( it.hasNext(), is( false ) );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotSetEntries()
    {
        map.entrySet().iterator().next().setValue( "" );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotRemove()
    {
        map.remove( "a" );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotRemoveNull()
    {
        map.remove( null );
    }

    @Test
    public void shouldNotHaveEqualEntry()
    {
        Map<String, String> map = new ImmutableMap<>( Collections.singletonMap( "k", "v" ) );
        Entry<String, String> e = map.entrySet().iterator().next();
        assertThat( e, is( not( (Entry<String, String>) null ) ) );
        assertThat( e, is( not( new Object() ) ) );
    }

    @Test
    public void shouldHaveEqualEntry()
    {
        Map<String, String> map = new ImmutableMap<>( Collections.singletonMap( "k", "v" ) );
        Entry<String, String> e = map.entrySet().iterator().next();
        assertThat( e, is( e ) );
        assertThat( e, is( (Entry<String, String>) new Node<>( "k", "v" ) ) );
    }
}
