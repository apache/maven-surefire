package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.util.CommandlineExecutor;
import org.apache.maven.surefire.extensions.util.CommandlineStreams;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.extensions.util.LineConsumerThread;
import org.apache.maven.surefire.shared.utils.cli.Commandline;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.nio.file.Paths;

import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Files.delete;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class CommandlineExecutorTest
{
    private CommandlineExecutor exec;
    private CommandlineStreams streams;
    private String baseDir;
    private LineConsumerThread out;

    @Before
    public void setUp() throws Exception
    {
        baseDir = System.getProperty( "user.dir" );

        delete( Paths.get( baseDir, "target", "CommandlineExecutorTest" ).toFile() );

        boolean createdDir = Paths.get( baseDir, "target", "CommandlineExecutorTest" )
            .toFile()
            .mkdirs();

        assertThat( createdDir )
            .isTrue();

        boolean createdFile = Paths.get( baseDir, "target", "CommandlineExecutorTest", "a.txt" )
            .toFile()
            .createNewFile();

        assertThat( createdFile )
            .isTrue();
    }

    @After
    public void tearDown() throws Exception
    {
        if ( out != null )
        {
            out.close();
        }
        exec.close();
        streams.close();
        delete( Paths.get( baseDir, "target", "CommandlineExecutorTest" ).toFile() );
    }

    @Test
    public void shouldExecuteNativeCommand() throws Exception
    {
        Closeable closer = mock( Closeable.class );
        Commandline cli = new Commandline( IS_OS_WINDOWS ? "dir" : "ls -la" );
        cli.setWorkingDirectory( Paths.get( baseDir, "target", "CommandlineExecutorTest" ).toFile() );
        CountdownCloseable countdownCloseable = new CountdownCloseable( closer, 1 );
        exec = new CommandlineExecutor( cli, countdownCloseable );
        streams = exec.execute();
        @SuppressWarnings( "unchecked" )
        EventHandler<String> consumer = mock( EventHandler.class );

        out = new LineConsumerThread( "std-out-fork-1", streams.getStdOutChannel(), consumer, countdownCloseable );
        out.start();
        exec.awaitExit();
        verify( consumer )
            .handleEvent( contains( "a.txt" ) );
    }
}
