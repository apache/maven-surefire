/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.api.booter;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.util.DefaultRunOrderCalculator;
import org.apache.maven.surefire.api.util.RunOrder;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@code balanced} run order distribution width is derived from both the parallel
 * {@code threadcount} and the {@code forkcount}.
 */
public class BaseProviderFactoryTest {

    @Test
    public void distributionWidthDefaultsToForkCountWhenNoThreadCount() {
        assertThat(distributedParallelism(properties(null, "3"))).isEqualTo(3);
    }

    @Test
    public void distributionWidthEqualsThreadCountWhenSingleFork() {
        assertThat(distributedParallelism(properties("4", "1"))).isEqualTo(4);
    }

    @Test
    public void distributionWidthMultipliesThreadCountAndForkCount() {
        assertThat(distributedParallelism(properties("4", "3"))).isEqualTo(12);
    }

    @Test
    public void distributionWidthDefaultsToOneWhenNoPropertiesSet() {
        assertThat(distributedParallelism(properties(null, null))).isEqualTo(1);
    }

    @Test
    public void distributionWidthClampsZeroForkCountToOne() {
        assertThat(distributedParallelism(properties("4", "0"))).isEqualTo(4);
    }

    private static Map<String, String> properties(String threadCount, String forkCount) {
        Map<String, String> providerProperties = new HashMap<>();
        if (threadCount != null) {
            providerProperties.put(ProviderParameterNames.THREADCOUNT_PROP, threadCount);
        }
        if (forkCount != null) {
            providerProperties.put(ProviderParameterNames.FORKCOUNT_PROP, forkCount);
        }
        return providerProperties;
    }

    private static int distributedParallelism(Map<String, String> providerProperties) {
        BaseProviderFactory factory = new BaseProviderFactory(true);
        factory.setProviderProperties(providerProperties);
        factory.setRunOrderParameters(new RunOrderParameters(new RunOrder[] {RunOrder.BALANCED}, new File(".")));
        RunOrderCalculator calculator = factory.getRunOrderCalculator();
        assertThat(calculator).isInstanceOf(DefaultRunOrderCalculator.class);
        return getInternalState(calculator, "distributedParallelism");
    }

    private static int getInternalState(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
