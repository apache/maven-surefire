package org.apache.maven.plugin.surefire.util.internal;

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


import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Kristian Rosenvold
 */
public class Java15BlockingQueue
    implements BlockingQueue
{
    private final java.util.concurrent.BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<String>();

    public void add( String object )
    {
        blockingQueue.add( object );
    }

    public String take()
        throws InterruptedException
    {
        return blockingQueue.take();
    }
}

