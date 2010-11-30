package org.apache.maven.surefire.booter;

import org.apache.maven.surefire.providerapi.DirectoryScannerParametersAware;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.ReporterConfigurationAware;
import org.apache.maven.surefire.providerapi.SurefireClassLoadersAware;
import org.apache.maven.surefire.providerapi.TestArtifactInfoAware;
import org.apache.maven.surefire.providerapi.TestRequestAware;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

import java.util.Properties;

/**
* @author Kristian Rosenvold
*/
class Foo
    implements DirectoryScannerParametersAware, TestRequestAware, ProviderPropertiesAware, ReporterConfigurationAware,
    SurefireClassLoadersAware, TestArtifactInfoAware
{
    DirectoryScannerParameters directoryScannerParameters;

    TestRequest testSuiteDefinition;

    Properties providerProperties;

    ReporterConfiguration reporterConfiguration;

    ClassLoader surefireClassLoader;

    ClassLoader testClassLoader;

    TestRequest testRequest;

    TestArtifactInfo testArtifactInfo;

    boolean called = false;

    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScanner )
    {
        this.directoryScannerParameters = directoryScanner;
        this.called = true;
    }


    public Boolean isCalled()
    {
        return Boolean.valueOf( called);
    }

    public void setTestSuiteDefinition( TestRequest testSuiteDefinition )
    {
        this.testSuiteDefinition = testSuiteDefinition;
        this.called = true;
    }

    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
        this.called = true;
    }

    public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        this.reporterConfiguration = reporterConfiguration;
        this.called = true;
    }

    public void setClassLoaders( ClassLoader surefireClassLoader, ClassLoader testClassLoader )
    {
        this.testClassLoader = testClassLoader;
        this.surefireClassLoader = surefireClassLoader;
        this.called = true;
    }

    public void setTestRequest( TestRequest testRequest1 )
    {
        this.testRequest = testRequest1;
        this.called = true;
    }

    public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        this.testArtifactInfo = testArtifactInfo;
        this.called = true;
    }
}
