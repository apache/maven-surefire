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


/**
 * @author Dipak Pawar
 */
public class RunOrderFactory
{
    public static final RunOrder ALPHABETICAL = new AlphabeticalRunOrder();

    public static final RunOrder BALANCED = new BalancedRunOrder();

    public static final RunOrder FAILEDFIRST = new FailedFirstRunOrder();

    public static final RunOrder FILESYSTEM = new FileSystemRunOrder();

    public static final RunOrder HOURLY = new HourlyRunOrder();

    public static final RunOrder RANDOM = new RandomRunOrder();

    public static final RunOrder REVERSE_ALPHABETICAL = new ReverseAlphabeticalRunOrder();
}
