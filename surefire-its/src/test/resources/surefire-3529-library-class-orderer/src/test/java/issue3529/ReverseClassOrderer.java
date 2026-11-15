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
package issue3529;

import java.util.Comparator;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Acts like a library orderer: sorts the classes by name, reversed.
 */
public class ReverseClassOrderer implements ClassOrderer {
    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(Comparator.comparing(ReverseClassOrderer::name).reversed());
    }

    private static String name(ClassDescriptor descriptor) {
        return descriptor.getTestClass().getName();
    }
}
