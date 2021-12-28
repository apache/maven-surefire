package org.apache.maven.plugin.surefire.booterclient.output;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.extensions.EventHandler;

import javax.annotation.Nonnull;

/**
 * The standard output logger for the output stream of the forked JMV,
 * see org.apache.maven.plugin.surefire.extensions.SurefireForkChannel.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public final class NativeStdOutStreamConsumer
    implements EventHandler<String>
{
    private final Object outStreamLock;

    public NativeStdOutStreamConsumer( Object outStreamLock )
    {
        this.outStreamLock = outStreamLock;
    }

    @Override
    public void handleEvent( @Nonnull String message )
    {
        synchronized ( outStreamLock )
        {
            System.out.println( message );
        }
    }
}
