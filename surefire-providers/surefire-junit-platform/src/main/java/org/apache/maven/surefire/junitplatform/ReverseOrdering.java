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
package org.apache.maven.surefire.junitplatform;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.commons.util.ClassUtils;

public class ReverseOrdering {

    public static class ReverseMethodOrder implements MethodOrderer {

        private static final Comparator<MethodDescriptor> COMPARATOR =
                Comparator.comparing((descriptor) -> descriptor.getMethod().getName());

        @Override
        public void orderMethods(MethodOrdererContext context) {
            context.getMethodDescriptors().sort(COMPARATOR);
            Collections.reverse(context.getMethodDescriptors());
        }

        private static String parameterList(Method method) {
            return ClassUtils.nullSafeToString(method.getParameterTypes());
        }

        @Override
        public Optional<ExecutionMode> getDefaultExecutionMode() {
            return MethodOrderer.super.getDefaultExecutionMode();
        }
    }

    public static class ReverseClassOrder implements ClassOrderer {

        private static final Comparator<ClassDescriptor> COMPARATOR =
                Comparator.comparing((descriptor) -> descriptor.getTestClass().getName());

        @Override
        public void orderClasses(ClassOrdererContext context) {
            context.getClassDescriptors().sort(COMPARATOR);
            Collections.reverse(context.getClassDescriptors());
        }
    }
}
