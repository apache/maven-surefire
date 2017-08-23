package org.apache.maven.plugin.surefire.runorder.api;

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

import org.apache.maven.surefire.testset.RunOrderParameters;

import java.util.Collection;
import java.util.List;

/**
 * Interface to be implemented to create RunOrder.
 * <p>
 * How to create your RunOrder?
 * <pre>
 * {@code
 * public class YourRunOrder implements RunOrder
 * {
 *
 *  &#064;Override
 *  public String getName()
 *  {
 *   return "runOrderName";
 *  }
 *
 *  &#064;Override
 *  public List&lt;Class&lt;?&gt;&gt; orderTestClasses( Collection&lt;Class&lt;?&gt;&gt; classes,
 *  RunOrderParameters runOrderParameters, int threadCount )
 *  {
 *   return new ArrayList&lt;Class&lt;?&gt;&gt;( classes ); // Add your logic for ordering test classes
 *  }
 * }
 * }
 * </pre>
 *
 * <p>
 * <b>How to load it during surefire execution?</b>
 * <p>
 * In your implementation of RunOrderProvider, add your instance of RunOrder to the list of RunOrder instances
 * using {@link org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider#getRunOrders()}.
 *
 * @author Dipak Pawar
 */
public interface RunOrder
{

    /**
     * Gives name of RunOrder.
     * <br>
     * Used to get respective instance of RunOrder from available RunOrder instances by
     * comparing name of RunOrder.
     *
     * @return String
     */
    String getName();

    /**
     * Orders testClasses as per runOrder.
     *
     * @param testClasses
     *     Collection of all test classes which are passed to given surefire run.
     * @param runOrderParameters instance of RunOrderParameters containing runOrders,
     *                           runStatisticsFile and map containing properties if any.
     *
     * @param threadCount number of threads to allocate for this execution.
     * @return List of Test Classes
     */
    List<Class<?>> orderTestClasses( Collection<Class<?>> testClasses, RunOrderParameters runOrderParameters,
                                     int threadCount );
}
