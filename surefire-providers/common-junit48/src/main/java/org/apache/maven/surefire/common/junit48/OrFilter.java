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
package org.apache.maven.surefire.common.junit48;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

final class OrFilter extends Filter {
    private final Filter[] filters;

    OrFilter(Filter[] filters) {
        this.filters = filters;
    }

    @Override
    public boolean shouldRun(Description description) {
        for (Filter filter : filters) {
            if (filter.shouldRun(description)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String describe() {
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < filters.length; i++) {
            description.append(filters[i].describe());
            if (i != filters.length - 1) {
                description.append(" OR ");
            }
        }
        return description.toString();
    }
}
