package org.apache.maven.plugin.surefire.runorder.model;

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

import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.surefire.testset.RunOrderParameters;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.ALPHABETICAL;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.REVERSE_ALPHABETICAL;


/**
 * RunOrder to order test classes alphabetical on even hours,
 * reverse alphabetical on odd hours.
 *
 * @author Dipak Pawar
 */
public class HourlyRunOrder implements RunOrder
{

    @Override
    public String getName()
    {
        return "hourly";
    }

    @Override
    public List<Class<?>> orderTestClasses( Collection<Class<?>> scannedClasses,
                                            RunOrderParameters runOrderParameters, int threadCount )
    {
        final int hour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        if ( hour % 2 == 0 )
        {
            return ALPHABETICAL.orderTestClasses( scannedClasses, runOrderParameters, threadCount );
        }
        else
        {
            return REVERSE_ALPHABETICAL.orderTestClasses( scannedClasses, runOrderParameters, threadCount );
        }
    }
}
