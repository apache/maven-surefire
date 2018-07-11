package org.apache.maven.surefire.its.jiras;

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

import java.io.File;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Assert;
import org.junit.Test;

public class Surefire1535TestNGParallelSuitesIT extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testParallelSuites() throws Exception 
    {
        File tmp1 = null;
        File tmp2 = null;
        try
        {
            tmp1 = File.createTempFile( getClass().getName(), "tmp" );
            tmp2 = File.createTempFile( getClass().getName(), "tmp" );
            OutputValidator validator = unpack("/surefire-1535-parallel-testng").maven()
                    .sysProp( "testNgVersion", "5.7" )
                    .sysProp( "testNgClassifier", "jdk15" )
                    .sysProp( "it.ParallelTest1", tmp1.getAbsolutePath() )
                    .sysProp( "it.ParallelTest2", tmp2.getAbsolutePath() )
                    .executeTest();
            Assert.assertFalse(tmp1.exists());
            Assert.assertFalse(tmp2.exists());
            validator.assertTestSuiteResults( 2, 0, 0, 0 );
        }
        finally
        {
            if (tmp1 != null)
            {
                tmp1.delete();
            }
            if (tmp2 != null)
            {
                tmp2.delete();
            }
        }
    }
}
