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

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Test listener to record all the failures during one run
 *
 * @author Qingzhou Luo
 */
public class JUnitTestFailureListener
    extends RunListener
{
    private final List<Failure> allFailures = new ArrayList<Failure>();

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        if ( failure != null )
        {
            allFailures.add( failure );
        }
    }

    public List<Failure> getAllFailures()
    {
        return allFailures;
    }

    public void reset()
    {
        allFailures.clear();
    }
}
