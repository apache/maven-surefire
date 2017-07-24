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

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * Copies input map in {@link #ImmutableMap(Map) constructor}, and Entries are linked and thread-safe.
 * The map is immutable with linear list of entries.
 *
 * @param <K> key
 * @param <V> value
 * @since 2.20
 */
public final class ImmutableMap<K, V>
        extends AbstractMap<K, V>
{
    private final Node<K, V> first;

    public ImmutableMap( Map<K, V> map )
    {
        Node<K, V> first = null;
        Node<K, V> previous = null;
        for ( Entry<K, V> e : map.entrySet() )
        {
            Node<K, V> node = new Node<K, V>( e.getKey(), e.getValue() );
            if ( first == null )
            {
                first = node;
            }
            else
            {
                previous.next = node;
            }
            previous = node;
        }
        this.first = first;
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        Set<Entry<K, V>> entries = new LinkedHashSet<Entry<K, V>>();
        Node<K, V> node = first;
        while ( node != null )
        {
            entries.add( node );
            node = node.next;
        }
        return unmodifiableSet( entries );
    }

    static final class Node<K, V>
            implements Entry<K, V>
    {
        final K key;
        final V value;
        volatile Node<K, V> next;

        Node( K key, V value )
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey()
        {
            return key;
        }

        @Override
        public V getValue()
        {
            return value;
        }

        @Override
        public V setValue( V value )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            Node<?, ?> node = (Node<?, ?>) o;

            return getKey() != null ? getKey().equals( node.getKey() ) : node.getKey() == null
                           && getValue() != null ? getValue().equals( node.getValue() ) : node.getValue() == null;

        }

        @Override
        public int hashCode()
        {
            int result = getKey() != null ? getKey().hashCode() : 0;
            result = 31 * result + ( getValue() != null ? getValue().hashCode() : 0 );
            return result;
        }
    }
}
