package org.codehaus.surefire;

import junit.framework.TestCase;

public class SimpleJUnitTest
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

    public void testFoo()
    {
        assertTrue( true );
    }
}
