package org.apache.maven.surefire.crb;

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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: benson
 * Date: 3/16/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClassRuleIT extends Assert {

    @ClassRule
    public static ExampleClassRule rule = new ExampleClassRule(ExampleClassRule.someStaticFunction());

    @Test
    public void dummyTest() {

    }

}
