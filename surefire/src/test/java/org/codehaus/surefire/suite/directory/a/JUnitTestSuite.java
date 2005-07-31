package org.codehaus.surefire.battery.directory.a;

import junit.framework.TestCase;
import org.codehaus.surefire.User;

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class JUnitTestSuite
    extends TestCase
{
    private User user;

    public void setUp()
    {
        user = new User();
    }

    public void tearDown()
    {
    }

    public void testUserFirstNameProperty()
        throws Exception
    {
        assertNotNull( user );

        user.setFirstName( "jason" );
        assertEquals( "jason", user.getFirstName() );
    }
}
