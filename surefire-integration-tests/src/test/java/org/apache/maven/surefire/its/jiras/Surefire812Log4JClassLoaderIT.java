package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;

/**
 * @author Kristian Rosenvold
 */
public class Surefire812Log4JClassLoaderIT
    extends SurefireIntegrationTestCase
{
    public void testJunit3ParallelBuildResultCount()
    {
        executeErrorFreeTest( "surefire-812-log4j-classloader", 1 );
    }
}
