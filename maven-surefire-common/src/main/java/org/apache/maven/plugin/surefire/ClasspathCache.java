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
package org.apache.maven.plugin.surefire;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.surefire.booter.Classpath;

/**
 * @author Kristian Rosenvold
 */
@Deprecated
public class ClasspathCache {
    private static final ConcurrentHashMap<String, Classpath> CLASSPATHS = new ConcurrentHashMap<>(4);

    public static Classpath getCachedClassPath(@Nonnull String artifactId) {
        return CLASSPATHS.get(artifactId);
    }

    public static void setCachedClasspath(@Nonnull String key, @Nonnull Classpath classpath) {
        CLASSPATHS.put(key, classpath);
    }

    public static Classpath setCachedClasspath(@Nonnull String key, @Nonnull Set<Artifact> artifacts) {
        Collection<String> files = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            files.add(artifact.getFile().getAbsolutePath());
        }
        Classpath classpath = new Classpath(files);
        setCachedClasspath(key, classpath);
        return classpath;
    }
}
