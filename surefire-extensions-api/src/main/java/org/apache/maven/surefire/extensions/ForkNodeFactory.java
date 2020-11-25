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

import org.apache.maven.surefire.api.fork.ForkNodeArguments;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * This is the plugin extension as a factory of {@link ForkChannel}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public interface ForkNodeFactory
{
    /**
     * Opens and closes the channel.
     *
     * @param arguments fork starter properties
     * @return specific implementation of the communication channel
     * @throws IOException if cannot open the channel
     */
    @Nonnull
    ForkChannel createForkChannel( @Nonnull ForkNodeArguments arguments ) throws IOException;
}
