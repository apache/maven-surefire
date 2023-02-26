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
package org.apache.maven.surefire.common.junit48.tests.group;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryA;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * ABCParameterizedTest.
 */
@Category(CategoryA.class)
@RunWith(Parameterized.class)
public class ABCParameterizedTest extends AbstractBCTest {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][] {{0}, {1}, {6}});
    }

    private int number;

    public ABCParameterizedTest(int number) {
        this.number = number;
    }

    @Test
    public void abc() {
        System.out.println("ABCTest#abc(" + number + ")");
    }
}
