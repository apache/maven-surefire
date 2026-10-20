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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.providerapi.ProviderRequirements;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaExports;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;

import static java.util.Collections.emptyList;
import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;

/**
 * The guesses Surefire makes about the module path when running with Maven 3.
 * <p>
 * Maven 3 doesn't say which dependencies belong on the module path, so Surefire works it out and adds the JVM
 * options the result needs to run. Maven 4 makes that choice itself, so these rules are only here for the Maven 3
 * way. {@link AbstractSurefireMojo} only uses this class from {@code newStartupConfigWithModularPath} and
 * {@code JUnitPlatformProviderInfo#getJpmsArguments}, so skipping them means skipping those two calls.
 */
final class ModulePathJpmsArguments {
    private ModulePathJpmsArguments() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Where the test path elements go, and the extra JVM arguments they need.
     */
    static final class Layout {
        private final Classpath classpath;
        private final Classpath modulepath;
        private final List<String[]> jpmsArguments;

        Layout(Classpath classpath, Classpath modulepath, List<String[]> jpmsArguments) {
            this.classpath = classpath;
            this.modulepath = modulepath;
            this.jpmsArguments = jpmsArguments;
        }

        Classpath getClasspath() {
            return classpath;
        }

        Classpath getModulepath() {
            return modulepath;
        }

        List<String[]> getJpmsArguments() {
            return jpmsArguments;
        }
    }

    /**
     * The main module has a module-info and holds the tests. Only the modules it requires go on the module path,
     * everything else, like the test engines and the provider, goes on the class path.
     *
     * @param locationManager works out the module path
     * @param testClasspath the whole test class path
     * @param mainModule the main module, which also holds the test classes
     * @param javaHome the JDK running the tests
     * @param testPackages the packages of the test classes
     * @param logger logs the path elements that could not be read
     * @return the class path, the module path and the arguments letting both sides reach each other
     * @throws IOException if the module path can't be worked out
     */
    static Layout forMainModule(
            LocationManager locationManager,
            Classpath testClasspath,
            JavaModuleDescriptor mainModule,
            String javaHome,
            Collection<String> testPackages,
            PluginConsoleLogger logger)
            throws IOException {
        ResolvePathsRequest<String> req = ResolvePathsRequest.ofStrings(testClasspath.getClassPath())
                .setIncludeAllProviders(true)
                .setJdkHome(javaHome)
                .setIncludeStatic(true)
                .setModuleDescriptor(mainModule);

        ResolvePathsResult<String> result = locationManager.resolvePaths(req);
        for (Entry<String, Exception> entry : result.getPathExceptions().entrySet()) {
            // Probably JDK version < 9. Other known causes: passing a non-jar or a corrupted jar.
            logger.warning("Exception for '" + entry.getKey() + "'.", entry.getValue());
        }

        return new Layout(
                new Classpath(result.getClasspathElements()),
                new Classpath(result.getModulepathElements().keySet()),
                of(result, mainModule.name(), testPackages));
    }

    /**
     * The tests have their own module-info. Everything goes on the module path.
     *
     * @param testClasspath the whole test class path
     * @return an empty class path and the whole test class path as module path
     */
    static Layout forTestModule(Classpath testClasspath) {
        return new Layout(emptyClasspath(), testClasspath, emptyList());
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

    /**
     * Opens JUnit Platform Commons to the provider on the class path when the tests have their own module-info.
     *
     * @param forkRequirements how the fork runs the tests
     * @return {@code --add-opens} arguments, possibly none
     */
    static List<String[]> junitPlatformArguments(ProviderRequirements forkRequirements) {
        if (!forkRequirements.isModularPath() || !forkRequirements.hasTestModuleDescriptor()) {
            return emptyList();
        }

        List<String[]> args = new ArrayList<>();
        args.add(
                new String[] {"--add-opens", "org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED"});
        args.add(
                new String[] {"--add-opens", "org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED"
                });
        return args;
    }
}
