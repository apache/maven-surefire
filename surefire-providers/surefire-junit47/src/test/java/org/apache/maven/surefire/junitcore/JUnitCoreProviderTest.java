package org.apache.maven.surefire.junitcore;

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

import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.ScannerFilter;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Boyles
 */
@RunWith( MockitoJUnitRunner.class )
public class JUnitCoreProviderTest
{
    @Mock
    private ProviderParameters providerParameters;
    @Mock
    private ScanResult scanResult;

    @Test
    public void doNotSortTestsIfNoRunOrderProvider()
    {
        Mockito.when( providerParameters.getScanResult() ).thenReturn( scanResult );
        Mockito.when( scanResult.applyFilter( Mockito.any( ScannerFilter.class ), Mockito.any( ClassLoader.class ) ) )
            .thenReturn( TestsToRun.fromClass( String.class ) );

        JUnitCoreProvider provider = new JUnitCoreProvider( providerParameters );
        Iterator<Class<?>> suites = provider.getSuites().iterator();

        assertTrue( suites.hasNext() );
        assertEquals( String.class, suites.next() );
        assertFalse( suites.hasNext() );
    }
}
