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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaExports;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;

/**
 * Extra JVM arguments for a fork that runs the tests inside the main module.
 * <p>
 * Only the modules the main module requires go on the module path. Everything else, like the test engines and
 * the provider, goes on the class path. These arguments let both sides reach each other.
 */
final class ModulePathJpmsArguments {
    private ModulePathJpmsArguments() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * @param result the module path and class path worked out for the main module
     * @param mainModule the main module, which also holds the test classes
     * @param testPackages the packages of the test classes
     * @return {@code --add-exports} and {@code --add-opens} arguments, possibly none
     */
    static List<String[]> of(ResolvePathsResult<String> result, String mainModule, Collection<String> testPackages) {
        SortedSet<String> exports = new TreeSet<>();
        SortedSet<String> modules = new TreeSet<>();
        Map<String, JavaModuleDescriptor> descriptors = result.getPathElements();
        Map<String, ?> modulePath = result.getModulepathElements();
        if (descriptors != null && modulePath != null) {
            for (String element : modulePath.keySet()) {
                JavaModuleDescriptor module = descriptors.get(element);
                if (module == null || module.name() == null) {
                    continue;
                }
                modules.add(module.name());
                // packages exported only to friend modules, e.g. org.junit.platform.commons.util,
                // while the friends and the provider live on the class path
                for (JavaExports export : module.exports()) {
                    Set<String> targets = export.targets();
                    if (targets != null && !targets.isEmpty()) {
                        exports.add(module.name() + "/" + export.source());
                    }
                }
            }
        }

        List<String[]> args = new ArrayList<>();
        for (String export : exports) {
            args.add(new String[] {"--add-exports", export + "=ALL-UNNAMED"});
        }

        // frameworks on the module path, e.g. JUnit when the main module requires it, reflect on the tests too
        modules.remove(mainModule);
        if (!modules.isEmpty()) {
            String targets = String.join(",", modules);
            for (String testPackage : testPackages) {
                args.add(new String[] {"--add-opens", mainModule + "/" + testPackage + "=" + targets});
            }
        }
        return args;
    }
}
