package org.apache.maven.surefire.testset;

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

import java.util.Set;

/**
 * Resolves string test patterns in object oriented patterns {@code P}.
 *
 * @param <P>    resolved atomic test, object oriented - not necessary to be a string
 * @param <C>    test class, or null if not mandatory
 * @param <M>    test method, or null if not mandatory
 */
public interface GenericTestPattern<P, C, M>
    extends TestFilter<C, M>
{
    boolean hasIncludedMethodPatterns();

    boolean hasExcludedMethodPatterns();

    boolean hasMethodPatterns();

    boolean isEmpty();

    String getPluginParameterTest();

    Set<P> getIncludedPatterns();

    Set<P> getExcludedPatterns();
}
