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
package org.apache.maven.surefire.junitcore.pc;

/**
 * Counts number of JUnit suites, classes and methods.
 *
 * @author tibor17 (Tibor Digana)
 * @see ParallelComputerBuilder
 * @since 2.17
 */
final class RunnerCounter {
    final long suites;

    final long classes;

    final long methods;

    RunnerCounter(long suites, long classes, long methods) {
        this.suites = suites;
        this.classes = classes;
        this.methods = methods;
    }
}
