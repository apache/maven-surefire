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
package org.apache.maven.plugin.surefire.booterclient;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.util.TempFileManager;
import org.apache.maven.surefire.booter.AbstractPathConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;

import static java.io.File.pathSeparatorChar;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.apache.maven.surefire.shared.utils.StringUtils.replace;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public class ModularClasspathForkConfiguration extends DefaultForkConfiguration {
    @SuppressWarnings("checkstyle:parameternumber")
    public ModularClasspathForkConfiguration(
            @Nonnull Classpath bootClasspath,
            @Nonnull File tempDirectory,
            @Nullable String debugLine,
            @Nonnull File workingDirectory,
            @Nonnull Properties modelProperties,
            @Nullable String argLine,
            @Nonnull Map<String, String> environmentVariables,
            @Nonnull String[] excludedEnvironmentVariables,
            boolean debug,
            @Nonnegative int forkCount,
            boolean reuseForks,
            @Nonnull Platform pluginPlatform,
            @Nonnull ConsoleLogger log,
            @Nonnull ForkNodeFactory forkNodeFactory) {
        super(
                bootClasspath,
                tempDirectory,
                debugLine,
                workingDirectory,
                modelProperties,
                argLine,
                environmentVariables,
                excludedEnvironmentVariables,
                debug,
                forkCount,
                reuseForks,
                pluginPlatform,
                log,
                forkNodeFactory);
    }

    @Override
    protected void resolveClasspath(
            @Nonnull Commandline cli,
            @Nonnull String startClass,
            @Nonnull StartupConfiguration config,
            @Nonnull File workingDirectory,
            @Nonnull File dumpLogDirectory)
            throws SurefireBooterForkException {
        try {
            AbstractPathConfiguration pathConfig = config.getClasspathConfiguration();

            ModularClasspathConfiguration modularClasspathConfiguration =
                    pathConfig.toRealPath(ModularClasspathConfiguration.class);

            ModularClasspath modularClasspath = modularClasspathConfiguration.getModularClasspath();

            boolean isMainDescriptor = modularClasspath.isMainDescriptor();
            String moduleName = modularClasspath.getModuleNameFromDescriptor();
            List<String> modulePath = modularClasspath.getModulePath();
            Collection<String> packages = modularClasspath.getPackages();
            File patchFile = modularClasspath.getPatchFile();
            List<String> classpath = toCompleteClasspath(config);

            File argsFile = createArgsFile(
                    moduleName,
                    modulePath,
                    classpath,
                    packages,
                    patchFile,
                    startClass,
                    isMainDescriptor,
                    config.getJpmsArguments());

            cli.createArg().setValue("@" + escapeToPlatformPath(argsFile.getAbsolutePath()));
        } catch (IOException e) {
            String error = "Error creating args file";
            InPluginProcessDumpSingleton.getSingleton().dumpException(e, error, dumpLogDirectory);
            throw new SurefireBooterForkException(error, e);
        }
    }

    @Nonnull
    File createArgsFile(
            @Nonnull String moduleName,
            @Nonnull List<String> modulePath,
            @Nonnull List<String> classPath,
            @Nonnull Collection<String> packages,
            File patchFile,
            @Nonnull String startClassName,
            boolean isMainDescriptor,
            @Nonnull List<String[]> providerJpmsArguments)
            throws IOException {
        File surefireArgs = TempFileManager.instance(getTempDirectory()).createTempFile("surefireargs", "");
        if (isDebug()) {
            getLogger().debug("Path to args file: " + surefireArgs.getCanonicalPath());
        } else {
            surefireArgs.deleteOnExit();
        }

        try (FileWriter io = new FileWriter(surefireArgs)) {
            StringBuilder args = new StringBuilder(64 * 1024);
            if (!modulePath.isEmpty()) {
                // https://docs.oracle.com/en/java/javase/11/tools/java.html#GUID-4856361B-8BFD-4964-AE84-121F5F6CF111
                args.append("--module-path").append(NL).append('"');

                for (Iterator<String> it = modulePath.iterator(); it.hasNext(); ) {
                    args.append(replace(it.next(), "\\", "\\\\"));
                    if (it.hasNext()) {
                        args.append(pathSeparatorChar);
                    }
                }

                args.append('"').append(NL);
            }

            if (!classPath.isEmpty()) {
                args.append("--class-path").append(NL).append('"');

                for (Iterator<String> it = classPath.iterator(); it.hasNext(); ) {
                    args.append(replace(it.next(), "\\", "\\\\"));
                    if (it.hasNext()) {
                        args.append(pathSeparatorChar);
                    }
                }

                args.append('"').append(NL);
            }

            if (isMainDescriptor) {
                args.append("--patch-module")
                        .append(NL)
                        .append(moduleName)
                        .append('=')
                        .append('"')
                        .append(replace(patchFile.getPath(), "\\", "\\\\"))
                        .append('"')
                        .append(NL);

                // Check for module-info-patch.args generated by maven-compiler-plugin 4.x
                File patchArgs = findModuleInfoPatchArgs(patchFile);
                if (patchArgs != null) {
                    // Merge the file's directives, except --add-reads/--add-modules
                    // which surefire manages itself (see appendModuleInfoPatchArgs)
                    appendModuleInfoPatchArgs(args, patchArgs, moduleName);
                }

                // Always auto-generate --add-opens for test packages (JUnit needs reflection access).
                // module-info-patch.maven cannot use ALL-UNNAMED in add-opens, so surefire handles this.
                for (String pkg : packages) {
                    args.append("--add-opens")
                            .append(NL)
                            .append(moduleName)
                            .append('/')
                            .append(pkg)
                            .append('=')
                            .append("ALL-UNNAMED")
                            .append(NL);
                }

                if (patchArgs == null) {
                    // Without module-info-patch.args, also auto-generate --add-reads
                    args.append("--add-reads")
                            .append(NL)
                            .append(moduleName)
                            .append('=')
                            .append("ALL-UNNAMED")
                            .append(NL);
                }
            }

            args.append("--add-modules").append(NL).append("ALL-MODULE-PATH").append(NL);

            for (String[] entries : providerJpmsArguments) {
                for (String entry : entries) {
                    args.append(entry).append(NL);
                }
            }

            args.append(startClassName);

            String argsFileContent = args.toString();

            if (isDebug()) {
                getLogger().debug("args file content:" + NL + argsFileContent);
            }

            io.write(argsFileContent);

            return surefireArgs;
        }
    }

    /**
     * Searches for module-info-patch.args generated by maven-compiler-plugin 4.x.
     * The file is expected at {@code target/test-classes/META-INF/maven/module-info-patch.args}
     * or within a module subdirectory at
     * {@code target/test-classes/<module>/META-INF/maven/module-info-patch.args}.
     *
     * @param patchFile the test classes directory (may be the module subdirectory)
     * @return the module-info-patch.args file, or null if not found
     */
    @Nullable
    private static File findModuleInfoPatchArgs(File patchFile) {
        // Direct location: target/test-classes/<module>/META-INF/maven/module-info-patch.args
        File argsFile = new File(patchFile, "META-INF/maven/module-info-patch.args");
        if (argsFile.isFile()) {
            return argsFile;
        }
        // Parent location: target/test-classes/META-INF/maven/module-info-patch.args
        File parent = patchFile.getParentFile();
        if (parent != null) {
            argsFile = new File(parent, "META-INF/maven/module-info-patch.args");
            if (argsFile.isFile()) {
                return argsFile;
            }
        }
        return null;
    }

    /**
     * Reads the module-info-patch.args file and appends its directives (e.g. --add-exports,
     * --add-opens) to the args builder. Exceptions are --add-reads and --add-modules, which
     * are skipped: they may reference named modules that surefire places on the classpath
     * rather than the module path. Surefire appends its own
     * {@code --add-reads <module>=ALL-UNNAMED} instead, and {@code --add-modules
     * ALL-MODULE-PATH} is generated by the caller.
     *
     * @param args the args builder to append to
     * @param patchArgs the module-info-patch.args file
     * @param moduleName the module name for surefire's own --add-reads
     * @throws IOException if the file cannot be read
     */
    private static void appendModuleInfoPatchArgs(StringBuilder args, File patchArgs, String moduleName)
            throws IOException {
        // explicit charset: the compiler writes the args file as UTF-8, the platform default may differ
        try (BufferedReader reader = Files.newBufferedReader(patchArgs.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Skip --add-reads and --add-modules from the file — surefire manages these itself.
                // The compiler-generated args may reference named modules that surefire places on
                // the classpath rather than the module-path, causing boot layer errors.
                if (line.startsWith("--add-reads") || line.startsWith("--add-modules")) {
                    if (line.equals("--add-reads") || line.equals("--add-modules")) {
                        // two-line form: the value is on the following line
                        reader.readLine();
                    }
                    continue;
                }
                args.append(line).append(NL);
            }
        }
        // Surefire always needs --add-reads <module>=ALL-UNNAMED for its classpath-based runner
        args.append("--add-reads")
                .append(NL)
                .append(moduleName)
                .append('=')
                .append("ALL-UNNAMED")
                .append(NL);
    }
}
