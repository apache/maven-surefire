package org.apache.maven.surefire.providerapi;

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

/**
 * Used to get additional provider-specific JVM arguments.
 *
 * @see ProviderInfo#getJpmsArguments(ProviderRequirements)
 */
public final class ProviderRequirements
{
    private final boolean modularPath;
    private final boolean mainModuleDescriptor;
    private final boolean testModuleDescriptor;

    public ProviderRequirements( boolean modularPath, boolean mainModuleDescriptor, boolean testModuleDescriptor )
    {
        this.modularPath = modularPath;
        this.mainModuleDescriptor = mainModuleDescriptor;
        this.testModuleDescriptor = testModuleDescriptor;
    }

    public boolean isModularPath()
    {
        return modularPath;
    }

    public boolean hasMainModuleDescriptor()
    {
        return mainModuleDescriptor;
    }

    public boolean hasTestModuleDescriptor()
    {
        return testModuleDescriptor;
    }
}
