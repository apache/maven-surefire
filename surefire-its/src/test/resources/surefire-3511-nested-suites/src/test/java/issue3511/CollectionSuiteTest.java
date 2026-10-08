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
package issue3511;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Builds its tests the way Guava's testlib does: one suite per feature set, each holding a suite per tester class.
 */
public class CollectionSuiteTest {
    public static Test suite() {
        TestSuite suite = new TestSuite("CollectionSuiteTest");
        for (String size : new String[] {"empty", "one", "several"}) {
            TestSuite features = new TestSuite("[size: " + size + "]");
            features.addTest(new TestSuite(SizeTester.class));
            features.addTest(new TestSuite(ContainsTester.class));
            suite.addTest(features);
        }
        return suite;
    }
}
