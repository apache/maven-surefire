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
package org.apache.maven.surefire.api.report;

import org.apache.maven.surefire.api.util.ReflectionUtils;

public class ReporterFactoryOptions {
    /**
     * provider such Junit5 may be running tests in parallel so report will stored depending on
     * {@link ReportEntry#getSourceName()}
     */
    private boolean statPerSourceName;

    public ReporterFactoryOptions() {}

    public ReporterFactoryOptions(boolean statPerSourceName) {
        this.statPerSourceName = statPerSourceName;
    }

    public boolean isStatPerSourceName() {
        return statPerSourceName;
    }

    public void setStatPerSourceName(boolean statPerSourceName) {
        this.statPerSourceName = statPerSourceName;
    }

    public Object clone(ClassLoader target) {
        try {
            Class<?> cls = ReflectionUtils.reloadClass(target, this);
            Object clone = cls.newInstance();

            cls.getMethod("setStatPerSourceName", boolean.class).invoke(clone, isStatPerSourceName());

            return clone;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e.getLocalizedMessage());
        }
    }
}
