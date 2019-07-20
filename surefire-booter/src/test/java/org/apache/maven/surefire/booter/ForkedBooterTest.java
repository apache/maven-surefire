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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Tests for {@link ForkedBooter}.
 */
public class ForkedBooterTest
{
    @Test
    public void shouldGenerateThreadDump() throws Exception
    {
        Collection<String> threadNames = new ArrayList<>();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for ( ThreadInfo threadInfo : threadMXBean.getThreadInfo( threadMXBean.getAllThreadIds(), 100 ) )
        {
            threadNames.add( threadInfo.getThreadName() );
        }

        String dump = invokeMethod( ForkedBooter.class, "generateThreadDump" );

        for ( String threadName : threadNames )
        {
            assertThat( dump )
                    .contains( "\"" + threadName + "\"" );
        }

        assertThat( dump )
                .contains( "   java.lang.Thread.State: " )
                .contains( "        at " );
    }

    @Test
    public void shouldFindCurrentProcessName() throws Exception
    {
        String process = ManagementFactory.getRuntimeMXBean().getName();
        String expected = invokeMethod( ForkedBooter.class, "getProcessName" );
        assertThat( process ).isEqualTo( expected );
    }

    @Test
    public void shouldNotBeDebugMode() throws Exception
    {
        boolean expected = invokeMethod( ForkedBooter.class, "isDebugging" );
        assertThat( expected ).isFalse();
    }

    @Test
    public void shouldReadSurefireProperties() throws Exception
    {
        File target = new File( System.getProperty( "user.dir", "target" ) );
        File tmpDir = new File( target, "ForkedBooterTest.1" );
        assertThat( tmpDir.mkdirs() )
                .isTrue();

        try
        {
            try ( InputStream is = invokeMethod( ForkedBooter.class, "createSurefirePropertiesIfFileExists",
                    tmpDir.getCanonicalPath(), "surefire.properties" ) )
            {
                assertThat( is )
                        .isNull();
            }

            File props = new File( tmpDir, "surefire.properties" );

            assertThat( props.createNewFile() )
                    .isTrue();

            FileUtils.write( props, "key=value", UTF_8 );

            try ( InputStream is2 = invokeMethod( ForkedBooter.class, "createSurefirePropertiesIfFileExists",
                    tmpDir.getCanonicalPath(), "surefire.properties" ) )
            {
                assertThat( is2 )
                        .isNotNull()
                        .isInstanceOf( FileInputStream.class );

                byte[] propsContent = new byte[20];
                int length = is2.read( propsContent );

                assertThat( new String( propsContent, 0, length ) )
                        .isEqualTo( "key=value" );
            }
        }
        finally
        {
            FileUtils.deleteDirectory( tmpDir );
        }
    }

    @Test
    public void shouldCreateScheduler() throws Exception
    {
        ScheduledExecutorService scheduler = null;
        try
        {
            scheduler = invokeMethod( ForkedBooter.class, "createPingScheduler" );
            assertThat( scheduler )
                    .isNotNull();
        }
        finally
        {
            if ( scheduler != null )
            {
                scheduler.shutdownNow();
            }
        }
    }
}