package org.apache.maven.plugin.surefire.report;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * tests for {@link TestSetStats}.
 */
@RunWith( PowerMockRunner.class )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class TestSetStatsTest
{
    @Mock
    private ReportEntry reportEntry;

    @Test
    public void shouldConcatenateWithTestGroup()
    {
        when( reportEntry.getNameWithGroup() )
                .thenReturn( "pkg.MyTest (my group)" );
        String actual = TestSetStats.concatenateWithTestGroup( buffer(), reportEntry, false );
        verify( reportEntry, times( 1 ) ).getNameWithGroup();
        verifyNoMoreInteractions( reportEntry );
        String expected = buffer().a( "pkg." ).strong( "MyTest (my group)" ).toString();
        assertThat( actual )
                .isEqualTo( expected );
    }

    @Test
    public void shouldConcatenateWithJUnit5TestGroup()
    {
        when( reportEntry.getReportNameWithGroup() )
                .thenReturn( "pkg.MyTest (my group)" );
        String actual = TestSetStats.concatenateWithTestGroup( buffer(), reportEntry, true );
        verify( reportEntry, times( 1 ) ).getReportNameWithGroup();
        verifyNoMoreInteractions( reportEntry );
        String expected = buffer().strong( "pkg.MyTest (my group)" ).toString();
        assertThat( actual )
                .isEqualTo( expected );
    }
}
