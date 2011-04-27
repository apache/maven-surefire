package org.apache.maven.surefire.util.internal;

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


import java.util.LinkedList;

/**
 * @author Kristian Rosenvold
 */
public class Java13BlockingQueue
    implements BlockingQueue
{
    private final java.util.List items = new LinkedList();

    public void add( Object object )
    {
        synchronized ( items )
        {
            items.add( object );
            items.notifyAll();
        }
    }

    public Object take()
        throws InterruptedException
    {
        synchronized ( items )
        {
            if ( items.size() == 0 )
            {
                items.wait();
            }

            return items.remove( 0 );
        }
    }
}

