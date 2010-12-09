package org.apache.maven.surefire.providerapi;

import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.DirectoryScanner;

import java.util.Properties;

/**
 * Injected into the providers upon provider construction. Allows the provider to request services and data it needs.
 *
 * @author Kristian Rosenvold
 */
public interface ProviderParameters
{
    /**
     * Provides a directory scanner that enforces the includes/excludes parameters that were passed to surefire.
     * See #getDirectoryScannerParameters for details
     * @return The directory scanner
     */
    DirectoryScanner getDirectoryScanner();

    /**
     * Provides features for creating reporting objects
     * @return A ReporterManagerFactory that allows the creation of one or more ReporterManagers
     */
    ReporterFactory getReporterFactory();

    /**
     * The raw parameters used in creating the directory scanner
     * @return The parameters
     */
    DirectoryScannerParameters getDirectoryScannerParameters();

    /**
     * The raw parameters used in creating the ReporterManagerFactory
     * @return The reporter configuration
     */
    ReporterConfiguration getReporterConfiguration();

    /**
     * Contains information about requested test suites or individual tests from the command line.
     * @return The testRequest
     */

    TestRequest getTestRequest();

    /**
     * The class loader for the tests
     * @return the classloader
     */
    ClassLoader getTestClassLoader();

    /**
     * The per-provider specific properties that may come all the way from the plugin's properties setting.
     * @return the provider specific properties
     */
    Properties getProviderProperties();

    /**
     * Artifact info about the artifact used to autodetect provider
     * @return The artifactinfo, or null if autodetect was not used.
     */
    TestArtifactInfo getTestArtifactInfo();
}
