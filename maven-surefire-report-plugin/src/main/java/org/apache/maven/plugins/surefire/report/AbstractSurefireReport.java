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
package org.apache.maven.plugins.surefire.report;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

import static java.util.Collections.addAll;
import static org.apache.maven.plugins.surefire.report.SurefireReportParser.hasReportFiles;

/**
 * Abstract base class for reporting test results using Surefire.
 *
 * @author Stephen Connolly
 */
public abstract class AbstractSurefireReport extends AbstractMavenReport {

    /**
     * If set to false, only failures are shown.
     */
    @Parameter(defaultValue = "true", required = true, property = "showSuccess")
    private boolean showSuccess;

    /**
     * Directories containing the XML Report files that will be parsed and rendered to HTML format.
     */
    @Parameter
    private File[] reportsDirectories;

    /**
     * (Deprecated, use reportsDirectories) This directory contains the XML Report files that will be parsed and
     * rendered to HTML format.
     */
    @Deprecated
    @Parameter
    private File reportsDirectory;

    /**
     * Link the violation line numbers to the (Test) Source XRef. Links will be created automatically if the JXR plugin is
     * being used.
     */
    @Parameter(property = "linkXRef", defaultValue = "true")
    private boolean linkXRef;

    /**
     * Location where Test Source XRef is generated for this project.
     * <br>
     * <strong>Default</strong>: {@link #getReportOutputDirectory()} + {@code /xref-test}
     */
    @Parameter
    private File xrefTestLocation;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     */
    @Parameter(defaultValue = "false", property = "aggregate")
    private boolean aggregate;

    /**
     * The current user system settings for use in Maven.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * Path for a custom bundle instead of using the default one. <br>
     * Using this field, you could change the texts in the generated reports.
     *
     * @since 3.1.0
     */
    @Parameter(defaultValue = "${basedir}/src/site/custom/surefire-report.properties")
    private String customBundle;

    /**
     * Internationalization component
     */
    @Component
    private I18N i18n;

    private List<File> resolvedReportsDirectories;

    /**
     * Whether the report should be generated or not.
     *
     * @return {@code true} if and only if the report should be generated.
     * @since 2.11
     */
    protected boolean isSkipped() {
        return false;
    }

    /**
     * Whether the report should be generated when there are no test results.
     *
     * @return {@code true} if and only if the report should be generated when there are no result files at all.
     * @since 2.11
     */
    protected boolean isGeneratedWhenNoResults() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeReport(Locale locale) {
        SurefireReportRenderer r = new SurefireReportRenderer(
                getSink(),
                getI18N(locale),
                getI18Nsection(),
                locale,
                getConsoleLogger(),
                getReportsDirectories(),
                linkXRef ? constructXrefLocation(xrefTestLocation, true) : null,
                showSuccess);
        r.render();
    }

    @Override
    public boolean canGenerateReport() {
        if (isSkipped()) {
            return false;
        }

        final List<File> reportsDirectories = getReportsDirectories();

        if (reportsDirectories == null) {
            return false;
        }

        if (!isGeneratedWhenNoResults()) {
            boolean atLeastOneDirectoryExists = false;
            for (Iterator<File> i = reportsDirectories.iterator(); i.hasNext() && !atLeastOneDirectoryExists; ) {
                atLeastOneDirectoryExists = hasReportFiles(i.next());
            }
            if (!atLeastOneDirectoryExists) {
                return false;
            }
        }
        return true;
    }

    private List<File> getReportsDirectories() {
        if (resolvedReportsDirectories != null) {
            return resolvedReportsDirectories;
        }

        resolvedReportsDirectories = new ArrayList<>();

        if (this.reportsDirectories != null) {
            addAll(resolvedReportsDirectories, this.reportsDirectories);
        }
        //noinspection deprecation
        if (reportsDirectory != null) {
            //noinspection deprecation
            resolvedReportsDirectories.add(reportsDirectory);
        }
        if (aggregate) {
            if (!project.isExecutionRoot()) {
                return null;
            }
            if (this.reportsDirectories == null) {
                if (reactorProjects.size() > 1) {
                    for (MavenProject mavenProject : getProjectsWithoutRoot()) {
                        resolvedReportsDirectories.add(getSurefireReportsDirectory(mavenProject));
                    }
                } else {
                    resolvedReportsDirectories.add(getSurefireReportsDirectory(project));
                }
            } else {
                // Multiple report directories are configured.
                // Let's see if those directories exist in each sub-module to fix SUREFIRE-570
                String parentBaseDir = getProject().getBasedir().getAbsolutePath();
                for (MavenProject subProject : getProjectsWithoutRoot()) {
                    String moduleBaseDir = subProject.getBasedir().getAbsolutePath();
                    for (File reportsDirectory1 : this.reportsDirectories) {
                        String reportDir = reportsDirectory1.getPath();
                        if (reportDir.startsWith(parentBaseDir)) {
                            reportDir = reportDir.substring(parentBaseDir.length());
                        }
                        File reportsDirectory = new File(moduleBaseDir, reportDir);
                        if (reportsDirectory.exists() && reportsDirectory.isDirectory()) {
                            getConsoleLogger().debug("Adding report dir: " + moduleBaseDir + reportDir);
                            resolvedReportsDirectories.add(reportsDirectory);
                        }
                    }
                }
            }
        } else {
            if (resolvedReportsDirectories.isEmpty()) {

                resolvedReportsDirectories.add(getSurefireReportsDirectory(project));
            }
        }
        return resolvedReportsDirectories;
    }

    /**
     * Gets the default surefire reports directory for the specified project.
     *
     * @param subProject the project to query.
     * @return the default surefire reports directory for the specified project.
     */
    protected abstract File getSurefireReportsDirectory(MavenProject subProject);

    private List<MavenProject> getProjectsWithoutRoot() {
        List<MavenProject> result = new ArrayList<>();
        for (MavenProject subProject : reactorProjects) {
            if (!project.equals(subProject)) {
                result.add(subProject);
            }
        }
        return result;
    }

    /**
     * @param locale The locale
     * @param key The key to search for
     * @return The text appropriate for the locale.
     */
    protected String getI18nString(Locale locale, String key) {
        return getI18N(locale).getString("surefire-report", locale, "report." + getI18Nsection() + '.' + key);
    }
    /**
     * @param locale The local.
     * @return I18N for the locale
     */
    protected I18N getI18N(Locale locale) {
        if (customBundle != null) {
            File customBundleFile = new File(customBundle);
            if (customBundleFile.isFile() && customBundleFile.getName().endsWith(".properties")) {
                if (!i18n.getClass().isAssignableFrom(CustomI18N.class)
                        || !i18n.getDefaultLanguage().equals(locale.getLanguage())) {
                    // first load
                    i18n = new CustomI18N(project, settings, customBundleFile, locale, i18n);
                }
            }
        }

        return i18n;
    }
    /**
     * @return The according string for the section.
     */
    protected abstract String getI18Nsection();

    /** {@inheritDoc} */
    public String getName(Locale locale) {
        return getI18nString(locale, "name");
    }

    /** {@inheritDoc} */
    public String getDescription(Locale locale) {
        return getI18nString(locale, "description");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getOutputName();

    protected final ConsoleLogger getConsoleLogger() {
        return new PluginConsoleLogger(getLog());
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    protected List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    // TODO Review, especially Locale.getDefault()
    private static class CustomI18N implements I18N {
        private final MavenProject project;

        private final Settings settings;

        private final String bundleName;

        private final Locale locale;

        private final I18N i18nOriginal;

        private ResourceBundle bundle;

        private static final Object[] NO_ARGS = new Object[0];

        CustomI18N(MavenProject project, Settings settings, File customBundleFile, Locale locale, I18N i18nOriginal) {
            super();
            this.project = project;
            this.settings = settings;
            this.locale = locale;
            this.i18nOriginal = i18nOriginal;
            this.bundleName = customBundleFile
                    .getName()
                    .substring(0, customBundleFile.getName().indexOf(".properties"));

            URLClassLoader classLoader = null;
            try {
                classLoader = new URLClassLoader(
                        new URL[] {customBundleFile.getParentFile().toURI().toURL()}, null);
            } catch (MalformedURLException e) {
                // could not happen.
            }

            this.bundle = ResourceBundle.getBundle(this.bundleName, locale, classLoader);
            if (!this.bundle.getLocale().getLanguage().equals(locale.getLanguage())) {
                this.bundle = ResourceBundle.getBundle(this.bundleName, Locale.getDefault(), classLoader);
            }
        }

        /** {@inheritDoc} */
        public String getDefaultLanguage() {
            return locale.getLanguage();
        }

        /** {@inheritDoc} */
        public String getDefaultCountry() {
            return locale.getCountry();
        }

        /** {@inheritDoc} */
        public String getDefaultBundleName() {
            return bundleName;
        }

        /** {@inheritDoc} */
        public String[] getBundleNames() {
            return new String[] {bundleName};
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle() {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle(String bundleName) {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle(String bundleName, String languageHeader) {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle(String bundleName, Locale locale) {
            return bundle;
        }

        /** {@inheritDoc} */
        public Locale getLocale(String languageHeader) {
            return new Locale(languageHeader);
        }

        /** {@inheritDoc} */
        public String getString(String key) {
            return getString(bundleName, locale, key);
        }

        /** {@inheritDoc} */
        public String getString(String key, Locale locale) {
            return getString(bundleName, locale, key);
        }

        /** {@inheritDoc} */
        public String getString(String bundleName, Locale locale, String key) {
            String value;

            if (locale == null) {
                locale = getLocale(null);
            }

            ResourceBundle rb = getBundle(bundleName, locale);
            value = getStringOrNull(rb, key);

            if (value == null) {
                // try to load default
                value = i18nOriginal.getString(bundleName, locale, key);
            }

            if (!value.contains("${")) {
                return value;
            }

            final RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
            try {
                interpolator.addValueSource(new EnvarBasedValueSource());
            } catch (final IOException e) {
                // In which cases could this happen? And what should we do?
            }

            interpolator.addValueSource(new PropertiesBasedValueSource(System.getProperties()));
            interpolator.addValueSource(new PropertiesBasedValueSource(project.getProperties()));
            interpolator.addValueSource(new PrefixedObjectValueSource("project", project));
            interpolator.addValueSource(new PrefixedObjectValueSource("pom", project));
            interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));

            try {
                value = interpolator.interpolate(value);
            } catch (final InterpolationException e) {
                // What does this exception mean?
            }

            return value;
        }

        /** {@inheritDoc} */
        public String format(String key, Object arg1) {
            return format(bundleName, locale, key, new Object[] {arg1});
        }

        /** {@inheritDoc} */
        public String format(String key, Object arg1, Object arg2) {
            return format(bundleName, locale, key, new Object[] {arg1, arg2});
        }

        /** {@inheritDoc} */
        public String format(String bundleName, Locale locale, String key, Object arg1) {
            return format(bundleName, locale, key, new Object[] {arg1});
        }

        /** {@inheritDoc} */
        public String format(String bundleName, Locale locale, String key, Object arg1, Object arg2) {
            return format(bundleName, locale, key, new Object[] {arg1, arg2});
        }

        /** {@inheritDoc} */
        public String format(String bundleName, Locale locale, String key, Object[] args) {
            if (locale == null) {
                locale = getLocale(null);
            }

            String value = getString(bundleName, locale, key);
            if (args == null) {
                args = NO_ARGS;
            }

            MessageFormat messageFormat = new MessageFormat("");
            messageFormat.setLocale(locale);
            messageFormat.applyPattern(value);

            return messageFormat.format(args);
        }

        private String getStringOrNull(ResourceBundle rb, String key) {
            if (rb != null) {
                try {
                    return rb.getString(key);
                } catch (MissingResourceException ignored) {
                    // intentional
                }
            }
            return null;
        }
    }
}
