package org.apache.maven.plugin.surefire.runorder.impl;

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

import org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider;

import java.util.Comparator;

class PriorityComparator
{
    static Comparator<RunOrderProvider> byPriority()
    {
        return new Comparator<RunOrderProvider>()
        {
            @Override
            public int compare( RunOrderProvider runOrderProvider1, RunOrderProvider runOrderProvider2 )
            {
                return runOrderProvider1.priority().compareTo( runOrderProvider2.priority() );
            }
        };
    }
}
