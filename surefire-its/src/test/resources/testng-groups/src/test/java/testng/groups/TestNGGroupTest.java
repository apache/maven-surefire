package testng.groups;

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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests grouping
 */
public class TestNGGroupTest
{
    private Object testObject;

    @BeforeClass( groups = "functional" )
    public void configureTest()
    {
        testObject = new Object();
    }

    @Test( groups = { "functional" } )
    public void isFunctional()
    {
        Assert.assertNotNull( testObject, "testObject is null" );
    }

    @Test( groups = { "functional", "notincluded" } )
    public void isFunctionalAndNotincluded()
    {
        Assert.assertNotNull( testObject, "testObject is null" );
    }

    @Test( groups = "notincluded" )
    public void isNotIncluded()
    {
        Assert.assertTrue( false );
    }

    @Test( groups = "abc-def" )
    public void isDashedGroup()
    {
    }

    @Test( groups = "foo.bar" )
    public void isFooBar()
    {
    }

    @Test( groups = "foo.zap" )
    public void isFooZap()
    {
    }

    @Test
    public void noGroup()
    {
    }
}
