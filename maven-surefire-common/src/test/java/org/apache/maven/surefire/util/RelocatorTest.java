/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.util;

import junit.framework.TestCase;
import org.apache.maven.plugin.surefire.util.Relocator;

/**
 * @author Kristian Rosenvold
 */
public class RelocatorTest extends TestCase {

    public void testFoo() {
        String cn = "org.apache.maven.surefire.report.ForkingConsoleReporter";
        assertEquals("org.apache.maven.shadefire.surefire.report.ForkingConsoleReporter", Relocator.relocate(cn));
    }

    public void testRelocation() {
        String org1 = "org.apache.maven.surefire.fooz.Baz";
        assertEquals("org.apache.maven.shadefire.surefire.fooz.Baz", Relocator.relocate(org1));
    }
}
