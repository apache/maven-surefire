package org.apache.maven.surefire.junitcore;

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

import org.apache.maven.surefire.common.junit4.Stoppable;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Calling {@link Stoppable#fireStopEvent()} if failure happens.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
final class JUnit47FailFastListener
    extends RunListener
{
    private final Stoppable stoppable;

    private final Queue<String> testClassNames = new ConcurrentLinkedQueue<String>();

    JUnit47FailFastListener( Stoppable stoppable )
    {
        this.stoppable = stoppable;
    }

    Queue<String> getRemainingTestClasses()
    {
        return testClassNames;
    }

    @Override
    public void testStarted( Description description )
        throws Exception
    {
        testClassNames.remove( description.getClassName() );
    }

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        stoppable.fireStopEvent();
    }
}
