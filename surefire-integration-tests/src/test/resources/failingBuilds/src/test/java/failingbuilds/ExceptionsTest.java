package failingbuilds;

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

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;

import junit.framework.TestCase;

public class ExceptionsTest
    extends TestCase
{
    public void testWithMultiLineExceptionBeingThrown()
    {
        throw new RuntimeException( "A very very long exception message indeed, which is to demonstrate truncation. It will be truncated somewhere\nA cat\nAnd a dog\nTried to make a\nParrot swim" );
    }
}