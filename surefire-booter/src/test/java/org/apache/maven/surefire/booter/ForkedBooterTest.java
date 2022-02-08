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

import org.apache.maven.surefire.shared.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Tests for {@link ForkedBooter}.
 */
@SuppressWarnings( "checkstyle:magicnumber" )
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
            scheduler = invokeMethod( ForkedBooter.class, "createScheduler", "thread name" );
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

    @Test( timeout = 10_000 )
    public void testBarrier1() throws Exception
    {
        Semaphore semaphore = new Semaphore( 2 );
        boolean acquiredOnePermit = invokeMethod( ForkedBooter.class, "acquireOnePermit", semaphore );

        assertThat( acquiredOnePermit ).isTrue();
        assertThat( semaphore.availablePermits() ).isEqualTo( 1 );
    }

    @Test
    public void testBarrier2() throws Exception
    {
        Semaphore semaphore = new Semaphore( 0 );
        Thread.currentThread().interrupt();
        try
        {
            boolean acquiredOnePermit = invokeMethod( ForkedBooter.class, "acquireOnePermit", semaphore );

            assertThat( acquiredOnePermit ).isFalse();
            assertThat( semaphore.availablePermits() ).isEqualTo( 0 );
        }
        finally
        {
            assertThat( Thread.interrupted() ).isFalse();
        }
    }

    @Test
    public void testScheduler() throws Exception
    {
        ScheduledThreadPoolExecutor executor = invokeMethod( ForkedBooter.class, "createScheduler", "thread name" );
        executor.shutdown();
        assertThat( executor.getCorePoolSize() ).isEqualTo( 1 );
        assertThat( executor.getMaximumPoolSize() ).isEqualTo( 1 );
    }

    @Test
    public void testIsDebug() throws Exception
    {
        boolean isDebug = invokeMethod( ForkedBooter.class, "isDebugging" );
        assertThat( isDebug ).isFalse();
    }

    @Test
    public void testPropsNotExist() throws Exception
    {
        String target = System.getProperty( "user.dir" );
        String file = "not exists";
        InputStream is = invokeMethod( ForkedBooter.class, "createSurefirePropertiesIfFileExists", target, file );
        assertThat( is ).isNull();
    }

    @Test
    public void testPropsExist() throws Exception
    {
        File props = File.createTempFile( "surefire", ".properties" );
        String target = props.getParent();
        String file = props.getName();
        FileUtils.write( props, "Hi", StandardCharsets.US_ASCII );
        try ( InputStream is =
                      invokeMethod( ForkedBooter.class, "createSurefirePropertiesIfFileExists", target, file ) )
        {
            assertThat( is ).isNotNull();
            byte[] data = new byte[5];
            int bytes = is.read( data );
            assertThat( bytes ).isEqualTo( 2 );
            assertThat( data[0] ).isEqualTo( (byte) 'H' );
            assertThat( data[1] ).isEqualTo( (byte) 'i' );
        }
    }

    @Test
    public void testThreadDump() throws Exception
    {
        String threads = invokeMethod( ForkedBooter.class, "generateThreadDump" );
        assertThat( threads )
                .isNotNull();
        assertThat( threads )
                .contains( "\"main\"" )
                .contains( "java.lang.Thread.State: RUNNABLE" );
    }
}
