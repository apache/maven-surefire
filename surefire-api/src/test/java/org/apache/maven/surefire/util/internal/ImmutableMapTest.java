package org.apache.maven.surefire.util.internal;

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

import org.apache.maven.surefire.util.internal.ImmutableMap.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 2.20
 */
public class ImmutableMapTest
{
    private ImmutableMap<String, String> map;

    @Before
    public void setUp() throws Exception
    {
        Map<String, String> backingMap = new HashMap<String, String>();
        backingMap.put( "a", "1" );
        backingMap.put( "x", null );
        backingMap.put( "b", "2" );
        backingMap.put( "c", "3" );
        backingMap.put( "", "" );
        backingMap.put( null, "1" );
        map = new ImmutableMap<String, String>( backingMap );
    }

    @Test
    public void testEntrySet() throws Exception
    {
        Set<Entry<String, String>> entries = map.entrySet();
        assertThat( entries, hasSize( 6 ) );
        assertThat( entries, hasItem( new Node<String, String>( "a", "1" ) ) );
        assertThat( entries, hasItem( new Node<String, String>( "x", null ) ) );
        assertThat( entries, hasItem( new Node<String, String>( "b", "2" ) ) );
        assertThat( entries, hasItem( new Node<String, String>( "c", "3" ) ) );
        assertThat( entries, hasItem( new Node<String, String>( "", "" ) ) );
        assertThat( entries, hasItem( new Node<String, String>( null, "1" ) ) );
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
}