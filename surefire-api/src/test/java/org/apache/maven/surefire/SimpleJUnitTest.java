package org.apache.maven.surefire;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
