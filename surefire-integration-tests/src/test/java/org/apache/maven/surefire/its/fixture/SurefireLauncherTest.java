package org.apache.maven.surefire.its.fixture;

import java.io.IOException;
import org.apache.maven.it.VerificationException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class SurefireLauncherTest
{
    @Test
    public void launcherGetsProperMethodName()
        throws IOException, VerificationException
    {
        String method = new SurefireLauncher( SurefireLauncherTest.class, "foo", "" ).getTestMethodName();
        assertEquals( "launcherGetsProperMethodName", method );

    }
}
