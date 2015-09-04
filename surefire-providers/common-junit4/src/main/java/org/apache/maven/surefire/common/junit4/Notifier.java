package org.apache.maven.surefire.common.junit4;

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

import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.surefire.util.internal.ConcurrencyUtils.countDownToZero;

/**
 * Extends {@link RunNotifier JUnit notifier},
 * encapsulates several different types of {@link RunListener JUnit listeners}, and
 * fires events to listeners.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class Notifier
    extends RunNotifier implements Stoppable
{
    private final Collection<RunListener> listeners = new ArrayList<RunListener>();

    private final AtomicInteger skipAfterFailureCount;

    private final JUnit4RunListener reporter;

    public Notifier( JUnit4RunListener reporter, int skipAfterFailureCount )
    {
        addListener( reporter );
        this.reporter = reporter;
        this.skipAfterFailureCount = new AtomicInteger( skipAfterFailureCount );
    }

    public void fireStopEvent()
    {
        if ( countDownToZero( skipAfterFailureCount ) )
        {
            pleaseStop();
        }

        reporter.testExecutionSkippedByUser();
    }

    @Override
    public void addListener( RunListener listener )
    {
        listeners.add( listener );
        super.addListener( listener );
    }

    public Notifier addListeners( Collection<RunListener> given )
    {
        for ( RunListener listener : given )
        {
            addListener( listener );
        }
        return this;
    }

    public Notifier addListeners( RunListener... given )
    {
        for ( RunListener listener : given )
        {
            addListener( listener );
        }
        return this;
    }

    @Override
    public void removeListener( RunListener listener )
    {
        listeners.remove( listener );
        super.removeListener( listener );
    }

    public void removeListeners()
    {
        for ( Iterator<RunListener> it = listeners.iterator(); it.hasNext(); )
        {
            RunListener listener = it.next();
            it.remove();
            super.removeListener( listener );
        }
    }
}
