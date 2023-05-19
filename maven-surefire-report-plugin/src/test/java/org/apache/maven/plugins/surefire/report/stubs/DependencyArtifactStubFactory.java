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
package org.apache.maven.plugins.surefire.report.stubs;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.ArtifactStubFactory;

public class DependencyArtifactStubFactory extends ArtifactStubFactory {
    private boolean flattenedPath = true;

    public DependencyArtifactStubFactory(File theWorkingDir, boolean theCreateFiles, boolean flattenedPath) {
        this(theWorkingDir, theCreateFiles);
        this.flattenedPath = flattenedPath;
    }

    public DependencyArtifactStubFactory(File theWorkingDir, boolean theCreateFiles) {
        super(theWorkingDir, theCreateFiles);
    }

    @Override
    public Artifact createArtifact(
            String groupId,
            String artifactId,
            VersionRange versionRange,
            String scope,
            String type,
            String classifier,
            boolean optional)
            throws IOException {
        File workingDir = getWorkingDir();

        if (!flattenedPath) {
            // don't use flatten directories, won't happen at runtime
            String path = groupId.replace('.', '/')
                    + '/'
                    + artifactId
                    + '/'
                    + ArtifactUtils.toSnapshotVersion(
                            versionRange.getRecommendedVersion().toString());
            setWorkingDir(new File(workingDir, path));
        }

        Artifact artifact = super.createArtifact(groupId, artifactId, versionRange, scope, type, classifier, optional);

        setWorkingDir(workingDir);

        return artifact;
    }
}
