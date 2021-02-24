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

package org.sample.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.fail;

public class StepDefs
{
    private static int testFailures = 0;

    @Given( "^I have some code$" )
    public void I_have_some_code()
        throws Throwable
    {
        // do nothing
    }

    @When( "^I run test$" )
    public void I_run_test()
        throws Throwable
    {
        // do nothing
    }

    @Then( "^I get a flake$" )
    public void I_get_a_flake()
        throws Throwable
    {
        // This test will error out with only one retry, but will pass with two
        if( testFailures < 2 ) {
            testFailures++;
            fail( "failing the test on purpose." );
        }
    }
}
