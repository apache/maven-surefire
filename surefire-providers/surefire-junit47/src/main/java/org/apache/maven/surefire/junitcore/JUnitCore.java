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

import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;

/**
 * JUnitCore solves bugs in original junit class {@link org.junit.runner.JUnitCore}.<p>
 * The notifier method {@link org.junit.runner.notification.RunNotifier#fireTestRunFinished}
 * is called anyway in finally block.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 * @see <a href="https://github.com/junit-team/junit/issues/1186">JUnit issue 1186</a>
 */
class JUnitCore
{
    private final Notifier notifier;

    JUnitCore( Notifier notifier )
    {
        this.notifier = notifier;
    }

    Result run( Runner runner )
        throws TestSetFailedException
    {
        Result result = new Result();
        RunListener listener = result.createListener();
        notifier.addFirstListener( listener );
        try
        {
            notifier.fireTestRunStarted( runner.getDescription() );
            runner.run( notifier );
        }
        catch ( Throwable e )
        {
            afterException( e );
        }
        finally
        {
            notifier.fireTestRunFinished( result );
            notifier.removeListener( listener );
            afterFinished();
        }
        return result;
    }

    protected void afterException( Throwable e )
        throws TestSetFailedException
    {
        throw new TestSetFailedException( e );
    }

    protected void afterFinished()
    {
    }
}
