package org.apache.maven.surefire.common.junit4;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4Reflector40Test
    extends TestCase
{
    @Test
    public void testGetAnnotatedIgnore()
    {
        JUnit4Reflector reflector = new JUnit4Reflector();
        Description desc = Description.createTestDescription( IgnoreWithDescription.class, "testSomething2" );
        Ignore annotatedIgnore = reflector.getAnnotatedIgnore( desc );
        Assert.assertNull( annotatedIgnore );
    }

    private static final String reason = "Ignorance is bliss";

    public static class IgnoreWithDescription
    {

        @Test
        @Ignore( reason )
        public void testSomething2()
        {
        }
    }
}


