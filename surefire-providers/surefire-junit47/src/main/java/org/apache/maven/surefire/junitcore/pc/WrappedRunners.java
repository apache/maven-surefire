package org.apache.maven.surefire.junitcore.pc;

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

import org.junit.runners.ParentRunner;

/**
 * We need to wrap runners in a suite and count children of these runners.
 * <br>
 * Old JUnit versions do not cache children after the first call of
 * {@link org.junit.runners.ParentRunner#getChildren()}.
 * Due to performance reasons, the children have to be observed just once.
 *
 * @author tibor17 (Tibor Digana)
 * @see ParallelComputerBuilder
 * @since 2.17
 */
final class WrappedRunners
{
    final ParentRunner wrappingSuite;

    final long embeddedChildrenCount;

    WrappedRunners( ParentRunner wrappingSuite, long embeddedChildrenCount )
    {
        this.wrappingSuite = wrappingSuite;
        this.embeddedChildrenCount = embeddedChildrenCount;
    }

    WrappedRunners()
    {
        this( null, 0 );
    }
}
