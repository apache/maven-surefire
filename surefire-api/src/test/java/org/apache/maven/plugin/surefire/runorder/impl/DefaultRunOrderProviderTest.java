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
import org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider;
import org.junit.Test;

import java.util.Collection;

import static org.apache.maven.plugin.surefire.runorder.impl.RunOrderLoader.asArray;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.*;
import static org.junit.Assert.assertArrayEquals;

public class DefaultRunOrderProviderTest {

    @Test
    public void shouldLoadAllDefaultRunOrders() {
        final RunOrderProvider runOrderLoader = RunOrderLoader.getRunOrderProvider();

        final Collection<RunOrder> runOrders = runOrderLoader.getRunOrders();

        assertArrayEquals(asArray(runOrders), new RunOrder[] {ALPHABETICAL, FILESYSTEM, HOURLY,
            RANDOM, REVERSE_ALPHABETICAL, BALANCED, FAILEDFIRST});
    }

    @Test
    public void shouldLoadFileSystemAsDefaultRunOrder() {
        final RunOrder[] defaultRunOrder = RunOrderLoader.defaultRunOrder();

        assertArrayEquals(defaultRunOrder, new RunOrder[] {FILESYSTEM});
    }
}