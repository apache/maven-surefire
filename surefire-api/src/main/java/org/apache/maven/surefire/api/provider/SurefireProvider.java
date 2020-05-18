package org.apache.maven.surefire.api.provider;

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

import java.lang.reflect.InvocationTargetException;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

/**
 * Interface to be implemented by all Surefire providers.
 * <br>
 * NOTE: This class is part of the proposed public api for surefire providers for 2.7. It may
 * still be subject to changes, even for minor revisions.
 * <br>
 * The api covers this interface and all the types reachable from it. And nothing else.
 * <br>
 * <br>
 * Called in one of three ways:
 * Forkmode = never: getSuites is not called, invoke is called with null parameter
 * Forkmode = once: getSuites is not called, invoke is called with null parameter
 * Forkmode anything else: getSuites is called, invoke is called on new provider instance for each item in getSuites
 * response.
 *
 * @author Kristian Rosenvold
 */
public interface SurefireProvider
{
    /**
     * Determines the number of forks.
     * <br>
     * Called when forkmode is different from "never" or "always", allows the provider to define
     * how to behave for the fork.
     *
     * @return An iterator that will trigger one fork per item
     */
    Iterable<Class<?>> getSuites();

    /**
     * Runs a forked test
     *
     * @param forkTestSet An item from the iterator in #getSuites. Will be null for forkmode never or always.
     *                    When this is non-null, the forked process will run only that test
     *                    and probably not scan the classpath
     * @return A result of the invocation
     * @throws ReporterException
     *          When reporting fails
     * @throws TestSetFailedException
     *          When testset fails
     * @throws InvocationTargetException fails in {@code ProviderFactory}
     */
    @SuppressWarnings( "checkstyle:redundantthrows" )
    RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException, InvocationTargetException;

    /**
     * Makes an attempt at cancelling the current run, giving the provider a chance to notify
     * reporting that the remaining tests have been cancelled due to timeout.
     * <br>
     * If the provider thinks it can terminate properly it is the responsibility of
     * the invoke method to return a RunResult with a booter code of failure.
     * <br>
     * It is up to the provider to find out how to implement this method properly.
     * A provider may also choose to not do anything at all in this method,
     * which means surefire will kill the forked process soon afterwards anyway.
     * <br>
     * Will be called on a different thread than the one calling invoke.
     */
    // Todo: Need to think a lot closer about how/if this works and if there is a use case for it.
    // Killing a process is slightly non-deterministic
    // And it
    void cancel();
}
