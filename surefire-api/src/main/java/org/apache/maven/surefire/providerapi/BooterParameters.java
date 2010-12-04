package org.apache.maven.surefire.providerapi;

import org.apache.maven.surefire.report.ReporterConfiguration;
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
public interface BooterParameters
{
    DirectoryScanner getDirectoryScanner();

    ReporterManagerFactory getReporterManagerFactory();

    DirectoryScannerParameters getDirectoryScannerParameters();

    ReporterConfiguration getReporterConfiguration();

    TestRequest getTestRequest();

    ClassLoader getTestClassLoader();

    Properties getProviderProperties();

    TestArtifactInfo getTestArtifactInfo();
}
