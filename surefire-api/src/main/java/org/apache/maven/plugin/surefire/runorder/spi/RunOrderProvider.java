package org.apache.maven.plugin.surefire.runorder.spi;

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

import java.util.Collection;
import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.plugin.surefire.runorder.api.RunOrderCalculator;
import org.apache.maven.surefire.testset.RunOrderParameters;

/**
 * This is the service provider interface (SPI) to override default runOrders provided by surefire.
 * <p>
 * <b>Packaging of RunOrderProvider Implementations</b>
 * <p>
 * Implementations of these run order provider are packaged using the Java Extension Mechanism as installed extensions.
 * A provider identifies itself with a provider-configuration file in the resource directory META-INF/services,
 * using the fully qualified provider interface class name as the file name.
 * <p>
 * For example, an implementation of the RunOrderProvider class should take the form of a jar file which contains
 * the file:
 * <p>
 * META-INF/services/org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider
 * <p>
 * And the file org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider should have a line such as:
 * <p>
   com.surefire.YourRunOrderProviderImpl
 * <p>
 * which is the fully qualified class name of the class implementing RunOrderProvider
 * <p>
 * <b>Loading RunOrder Providers</b>
 * <p>
 * RunOrder providers are loaded using
 * {@link org.apache.maven.plugin.surefire.runorder.impl.RunOrderLoader#getRunOrderProvider()} when needed.
 *
 * If a more than one implementation of provider is present on classpath, implementation with greater priority is used
 * others are ignored.
 *
 * If no implementation of provider is on classpath, default implementation of RunOrderProvider is used.
 *
 * see {@link org.apache.maven.plugin.surefire.runorder.impl.DefaultRunOrderProvider }).
 *
 *
 * @author Dipak Pawar
 */
public interface RunOrderProvider
{

    /**
     * Gives all supported RunOrder Implementations to use.
     *
     * @return A list of RunOrder Implementation.
     *
     * see {@link RunOrder}
     */
    Collection<RunOrder> getRunOrders();

    /**
     * Provides priority for RunOrderProvider
     *
     * @return An integer
     */
    Integer priority();

    /**
     * Creates RunOrderCalculator instance for ordering testClasses.
     *
     * @param runOrderParameters instance of RunOrderParameters containing runOrders,
     *                           runStatisticsFile and map containing properties if any.
     * @param threadCount number of threads to allocate for this execution.
     *
     * @return An instance of RunOrderCalculator.
     */
    RunOrderCalculator createRunOrderCalculator( RunOrderParameters runOrderParameters, int threadCount );

    /**
     * Gives RunOrder Implementations to use by default if no runOrder is specified in surefire configuration.
     *
     * All RunOrder Implementations using as default should be already provided using
     * {@link RunOrderProvider#getRunOrders()}
     *
     * @return A list of RunOrder Implementation.
     */
    Collection<RunOrder> defaultRunOrder();
}
