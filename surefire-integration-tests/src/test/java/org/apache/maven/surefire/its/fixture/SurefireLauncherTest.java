package org.apache.maven.surefire.its.fixture;

import org.apache.maven.it.VerificationException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class SurefireLauncherTest {
    @Test
    public void launcherGetsProperMethodName() throws IOException, VerificationException {
        String method = new SurefireLauncher(SurefireLauncherTest.class, "foo", "").getTestMethodName();
        assertEquals( "launcherGetsProperMethodName", method);

    }
}
