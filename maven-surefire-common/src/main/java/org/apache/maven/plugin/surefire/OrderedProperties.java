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

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * A properties implementation that preserves insertion order.
 */
public class OrderedProperties
    extends Properties
{
    private final LinkedHashSet<Object> items = new LinkedHashSet<Object>();

    public OrderedProperties()
    {
    }

    public OrderedProperties( Properties source )
    {
        if ( source != null )
        {
            this.putAll( source );
        }
    }

    @Override
    public synchronized Object put( Object key, Object value )
    {
        items.add( key );
        return super.put( key, value );
    }

    @Override
    public synchronized Object remove( Object key )
    {
        items.remove( key );
        return super.remove( key );
    }

    @Override
    public synchronized void clear()
    {
        items.clear();
        super.clear();
    }

    public synchronized Enumeration<Object> keys()
    {
        return Collections.enumeration( items );
    }

}
