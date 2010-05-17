package org.apache.maven.plugin.surefire;

import java.io.File;

/**
 * The parameters required to report on a surefire execution.
 *
 * @author Stephen Connolly
 */
public interface SurefireReportParameters
{
    boolean isSkipTests();

    void setSkipTests( boolean skipTests );

    boolean isSkipExec();

    void setSkipExec( boolean skipExec );

    boolean isSkip();

    void setSkip( boolean skip );

    boolean isTestFailureIgnore();

    void setTestFailureIgnore( boolean testFailureIgnore );

    File getBasedir();

    void setBasedir( File basedir );

    File getTestClassesDirectory();

    void setTestClassesDirectory( File testClassesDirectory );

    File getReportsDirectory();

    void setReportsDirectory( File reportsDirectory );

    Boolean getFailIfNoTests();

    void setFailIfNoTests( Boolean failIfNoTests );
}
