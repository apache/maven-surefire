package org.apache.maven.surefire.common.junit4;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4Reflector40Test
    extends TestCase
{
    public void testGetAnnotatedIgnore()
    {
        Description desc = Description.createTestDescription( IgnoreWithDescription.class, "testSomething2" );
        Ignore annotatedIgnore = JUnit4Reflector.getAnnotatedIgnore( desc );
        assertNull( annotatedIgnore );
    }

    private static final String REASON = "Ignorance is bliss";

    /**
     *
     */
    public static class IgnoreWithDescription
    {

        @Test
        @Ignore( REASON )
        public void testSomething2()
        {
        }
    }

    public void testCreateIgnored()
    {
        Ignore ignore = JUnit4Reflector.createIgnored( "error" );
        assertNotNull( ignore );
        assertNotNull( ignore.value() );
        assertEquals( "error", ignore.value() );
    }

    public void testCreateDescription()
    {
        Ignore ignore = JUnit4Reflector.createIgnored( "error" );
        Description description = JUnit4Reflector.createDescription( "exception", ignore );
        assertEquals( "exception", description.getDisplayName() );
        assertEquals( "exception", description.toString() );
        assertEquals( 0, description.getChildren().size() );
        // JUnit 4 description does not get annotations
        Ignore annotatedIgnore = JUnit4Reflector.getAnnotatedIgnore( description );
        assertNull( annotatedIgnore );
    }

    public void testCreatePureDescription()
    {
        Description description = JUnit4Reflector.createDescription( "exception" );
        assertEquals( "exception", description.getDisplayName() );
        assertEquals( "exception", description.toString() );
        assertEquals( 0, description.getChildren().size() );
    }
}


