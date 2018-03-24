package surefire.testcase;
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

import java.util.Arrays;
import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Surefire JunitParams test.
 */
@RunWith( JUnitParamsRunner.class )
public class JunitParamsTest
{

    @Parameters( method = "parameters" )
    @Test
    public void testSum( int a, int b, int expected )
    {
        assertThat( a + b, equalTo( expected ) );
    }

    public Collection<Integer[]> parameters()
    {
        Integer[][] parameters = { { 1, 2, 3 }, { 2, 3, 5 }, { 3, 4, 7 }, };
        return Arrays.asList( parameters );
    }
}
