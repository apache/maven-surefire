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

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static org.junit.Assert.fail;

public class StepDefs
{
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

    @Then( "^I get no failures$" )
    public void I_get_no_failures()
        throws Throwable
    {
        // do nothing
    }

    @Then( "^I get a failure$" )
    public void I_get_a_failure()
        throws Throwable
    {
        fail( "failing the test on purpose." );
    }
}
