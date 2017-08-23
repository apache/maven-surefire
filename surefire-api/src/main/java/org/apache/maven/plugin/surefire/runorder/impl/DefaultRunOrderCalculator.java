package org.apache.maven.plugin.surefire.runorder.impl;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.plugin.surefire.runorder.api.RunOrderCalculator;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.util.TestsToRun;

/**
 * Applies the final runorder of the tests
 *
 * @author Kristian Rosenvold
 */
public class DefaultRunOrderCalculator
    implements RunOrderCalculator
{
    private final RunOrder[] runOrders;

    private final RunOrderParameters runOrderParameters;

    private final int threadCount;

    public DefaultRunOrderCalculator( RunOrderParameters runOrderParameters, int threadCount )
    {
        this.runOrderParameters = runOrderParameters;
        this.runOrders = runOrderParameters.getRunOrders();
        this.threadCount = threadCount;
    }

    @Override
    @SuppressWarnings( "checkstyle:magicnumber" )
    public TestsToRun orderTestClasses( TestsToRun scannedClasses )
    {
        if ( runOrders == null || runOrders.length == 0 )
        {
            return  scannedClasses;
        }

        List<Class<?>> testClasses = new ArrayList<Class<?>>( 512 );

        for ( Class<?> scannedClass : scannedClasses )
        {
            testClasses.add( scannedClass );
        }

        final Collection<Class<?>> classes = runOrders[0].orderTestClasses( testClasses, runOrderParameters,
                threadCount );

        return new TestsToRun( new LinkedHashSet<Class<?>>( classes ) );
    }
}
