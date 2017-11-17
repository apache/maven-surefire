package org.apache.maven.surefire.booter;

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

import java.io.File;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

/**
 * Jigsaw class-path and module-path.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public final class ModularClasspath
{
    private final File moduleDescriptor;
    private final List<String> modulePath;
    private final Collection<String> packages;
    private final File patchFile;

    public ModularClasspath( File moduleDescriptor, List<String> modulePath, Collection<String> packages,
                             File patchFile )
    {
        this.moduleDescriptor = moduleDescriptor;
        this.modulePath = modulePath;
        this.packages = packages;
        this.patchFile = patchFile;
    }

    public File getModuleDescriptor()
    {
        return moduleDescriptor;
    }

    public List<String> getModulePath()
    {
        return unmodifiableList( modulePath );
    }

    public Collection<String> getPackages()
    {
        return unmodifiableCollection( packages );
    }

    public File getPatchFile()
    {
        return patchFile;
    }
}
