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

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.cutTestClassAndMethod;
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
    extends RunNotifier
{
    private final Collection<RunListener> listeners = new ArrayList<>();

    private final Queue<String> testClassNames = new ConcurrentLinkedQueue<>();

    private final AtomicInteger skipAfterFailureCount;

    private final JUnit4RunListener reporter;

    private volatile boolean failFast;

    public Notifier( JUnit4RunListener reporter, int skipAfterFailureCount )
    {
        addListener( reporter );
        this.reporter = reporter;
        this.skipAfterFailureCount = new AtomicInteger( skipAfterFailureCount );
    }

    private Notifier()
    {
        reporter = null;
        skipAfterFailureCount = null;
    }

    public static Notifier pureNotifier()
    {
        return new Notifier()
        {
            @Override
            public void asFailFast( @SuppressWarnings( { "unused", "checkstyle:hiddenfieldcheck" } ) boolean failFast )
            {
                throw new UnsupportedOperationException( "pure notifier" );
            }
        };
    }

    public void asFailFast( boolean enableFailFast )
    {
        failFast = enableFailFast;
    }

    public final boolean isFailFast()
    {
        return failFast;
    }

    @Override
    @SuppressWarnings( "checkstyle:redundantthrowscheck" ) // checkstyle is wrong here, see super.fireTestStarted()
    public final void fireTestStarted( Description description ) throws StoppedByUserException
    {
        // If fireTestStarted() throws exception (== skipped test), the class must not be removed from testClassNames.
        // Therefore this class will be removed only if test class started with some test method.
        super.fireTestStarted( description );
        if ( !testClassNames.isEmpty() )
        {
            testClassNames.remove( cutTestClassAndMethod( description ).getClazz() );
        }
    }

    @Override
    public final void fireTestFailure( Failure failure )
    {
        if ( failFast )
        {
            fireStopEvent();
        }
        super.fireTestFailure( failure );
    }

    @Override
    public final void addListener( RunListener listener )
    {
        listeners.add( listener );
        super.addListener( listener );
    }

    public final Notifier addListeners( Collection<RunListener> given )
    {
        for ( RunListener listener : given )
        {
            addListener( listener );
        }
        return this;
    }

    @SuppressWarnings( "unused" )
    public final Notifier addListeners( RunListener... given )
    {
        for ( RunListener listener : given )
        {
            addListener( listener );
        }
        return this;
    }

    @Override
    public final void removeListener( RunListener listener )
    {
        listeners.remove( listener );
        super.removeListener( listener );
    }

    public final void removeListeners()
    {
        for ( Iterator<RunListener> it = listeners.iterator(); it.hasNext(); )
        {
            RunListener listener = it.next();
            it.remove();
            super.removeListener( listener );
        }
    }

    public final Queue<String> getRemainingTestClasses()
    {
        return failFast ? testClassNames : null;
    }

    public final void copyListenersTo( Notifier copyTo )
    {
        copyTo.addListeners( listeners );
    }

    /**
     * Fire stop even to plugin process and/or call {@link org.junit.runner.notification.RunNotifier#pleaseStop()}.
     */
    private void fireStopEvent()
    {
        if ( countDownToZero( skipAfterFailureCount ) )
        {
            pleaseStop();
        }

        reporter.testExecutionSkippedByUser();
    }
}
