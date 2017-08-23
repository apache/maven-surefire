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

import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.plugin.surefire.runorder.api.RunOrderCalculator;
import org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider;
import org.apache.maven.surefire.testset.RunOrderParameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.ALPHABETICAL;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.BALANCED;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.FAILEDFIRST;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.FILESYSTEM;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.HOURLY;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.RANDOM;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.REVERSE_ALPHABETICAL;

/**
 * Provides Default implementation of RunOrderProvider for Surefire.
 *
 * @author Dipak Pawar
 */
public class DefaultRunOrderProvider implements RunOrderProvider
{

    @Override
    public Collection<RunOrder> getRunOrders()
    {
        return Arrays.asList( ALPHABETICAL, FILESYSTEM, HOURLY,
            RANDOM, REVERSE_ALPHABETICAL, BALANCED, FAILEDFIRST );
    }

    @Override
    public Integer priority()
    {
        return 0;
    }

    @Override
    public RunOrderCalculator createRunOrderCalculator( RunOrderParameters runOrderParameters, int threadCount )
    {
        return new DefaultRunOrderCalculator( runOrderParameters, threadCount );
    }

    @Override
    public Collection<RunOrder> defaultRunOrder()
    {
        return Collections.singletonList( FILESYSTEM );
    }
}
