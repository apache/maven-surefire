package org.apache.maven.surefire.report;

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

import org.apache.maven.surefire.api.util.internal.ClassMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

/**
 * Creates an index for class/method.
 * Returns ThreadLocal index if created before.
 */
public final class ClassMethodIndexer
{
    private final AtomicInteger classIndex = new AtomicInteger( 1 );
    private final AtomicInteger methodIndex = new AtomicInteger( 1 );
    private final Map<ClassMethod, Long> testIdMapping = new ConcurrentHashMap<>();
    private final ThreadLocal<Long> testLocalMapping = new ThreadLocal<>();

    public long indexClassMethod( String clazz, String method )
    {
        ClassMethod key = new ClassMethod( requireNonNull( clazz ), method );
        return testIdMapping.computeIfAbsent( key, cm ->
        {
            Long classId = testIdMapping.get( new ClassMethod( requireNonNull( clazz ), null ) );
            long c = classId == null ? ( ( (long) classIndex.getAndIncrement() ) << 32 ) : classId;
            int m = method == null ? 0 : methodIndex.getAndIncrement();
            long id = c | m;
            testLocalMapping.set( id );
            return id;
        } );
    }

    public long indexClass( String clazz )
    {
        return indexClassMethod( clazz, null );
    }

    public Long getLocalIndex()
    {
        return testLocalMapping.get();
    }
}
