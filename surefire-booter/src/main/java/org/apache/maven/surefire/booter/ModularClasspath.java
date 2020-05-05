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

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Jigsaw class-path and module-path.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public final class ModularClasspath
{
    private final String moduleNameFromDescriptor;
    private final List<String> modulePath;
    private final Collection<String> packages;
    private final File patchFile;
    private final boolean isMainDescriptor;

    public ModularClasspath( @Nonnull String moduleNameFromDescriptor,
                             @Nonnull List<String> modulePath,
                             @Nonnull Collection<String> packages,
                             File patchFile,
                             boolean isMainDescriptor )
    {
        this.moduleNameFromDescriptor = moduleNameFromDescriptor;
        this.modulePath = modulePath;
        this.packages = packages;
        this.patchFile =
            isMainDescriptor ? requireNonNull( patchFile, "patchFile should not be null" ) : patchFile;
        this.isMainDescriptor = isMainDescriptor;
    }

    @Nonnull
    public String getModuleNameFromDescriptor()
    {
        return moduleNameFromDescriptor;
    }

    @Nonnull
    public List<String> getModulePath()
    {
        return unmodifiableList( modulePath );
    }

    @Nonnull
    public Collection<String> getPackages()
    {
        return unmodifiableCollection( packages );
    }

    public File getPatchFile()
    {
        return patchFile;
    }

    public boolean isMainDescriptor()
    {
        return isMainDescriptor;
    }
}
