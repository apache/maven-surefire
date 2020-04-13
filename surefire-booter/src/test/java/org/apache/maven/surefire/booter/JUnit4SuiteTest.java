package org.apache.maven.surefire.booter;

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

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelDecoderTest;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoderTest;

/**
 * Adapt the JUnit4 tests which use only annotations to the JUnit3 test suite.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
public class JUnit4SuiteTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new JUnit4TestAdapter( CommandReaderTest.class ) );
        suite.addTest( new JUnit4TestAdapter( PpidCheckerTest.class ) );
        suite.addTest( new JUnit4TestAdapter( SystemUtilsTest.class ) );
        suite.addTest( new JUnit4TestAdapter( IsolatedClassLoaderTest.class ) );
        suite.addTest( new JUnit4TestAdapter( ForkedBooterTest.class ) );
        suite.addTest( new JUnit4TestAdapter( ForkedBooterMockTest.class ) );
        suite.addTest( new JUnit4TestAdapter( BooterDeserializerTest.class ) );
        suite.addTestSuite( ClasspathTest.class );
        suite.addTestSuite( PropertiesWrapperTest.class );
        suite.addTest( new JUnit4TestAdapter( LegacyMasterProcessChannelDecoderTest.class ) );
        suite.addTest( new JUnit4TestAdapter( LegacyMasterProcessChannelEncoderTest.class ) );
        suite.addTestSuite( SurefireReflectorTest.class );
        return suite;
    }
}
