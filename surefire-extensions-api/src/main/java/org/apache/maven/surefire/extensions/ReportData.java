package org.apache.maven.surefire.extensions;

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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.surefire.api.report.UniqueID;
import org.apache.maven.surefire.extensions.testoperations.TestOperation;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 *
 */
public final class ReportData
{
    private final List<TestOperation<?>> operations = new ArrayList<>();
    private final List<TestOperation<?>> rerunOperations = new ArrayList<>();

    public void addOperation( TestOperation<?> op )
    {
        operations.add( requireNonNull( op ) );
    }

    public void addRetryOperation( TestOperation<?> op )
    {
        rerunOperations.add( requireNonNull( op ) );
    }

    public Set<UniqueID> getIds()
    {
        return operations.stream()
            .map( TestOperation::getSourceId )
            .collect( toCollection( LinkedHashSet::new ) );
    }

    public List<TestOperation<?>> filterOperations( UniqueID sourceId )
    {
        return filterBySourceId( sourceId, operations );
    }

    public List<TestOperation<?>> filterRerunOperations( UniqueID sourceId )
    {
        return filterBySourceId( sourceId, rerunOperations );
    }

    public void removeSourceId( UniqueID sourceId )
    {
        removeBySourceId( sourceId, operations.iterator() );
        removeBySourceId( sourceId, rerunOperations.iterator() );
    }

    private List<TestOperation<?>> filterBySourceId( UniqueID sourceId, List<TestOperation<?>> sources )
    {
        return sources.stream()
            .filter( op -> op.getSourceId().equals( sourceId ) )
            .collect( toList() );
    }

    private void removeBySourceId( UniqueID sourceId, Iterator<TestOperation<?>> it )
    {
        for ( TestOperation<?> op; it.hasNext(); )
        {
            op = it.next();
            if ( op.getSourceId().equals( sourceId ) )
            {
                it.remove();
            }
        }
    }
}
