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

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;

import static org.apache.maven.surefire.booter.BooterConstants.PROCESS_CHECKER;
import static org.apache.maven.surefire.booter.BooterConstants.PROVIDER_CONFIGURATION;
import static org.apache.maven.surefire.booter.BooterConstants.USESYSTEMCLASSLOADER;
import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class BooterDeserializerTest
{
    @Test
    public void testStartupConfiguration() throws IOException
    {
        InputStream is = new StringBufferInputStream( PROCESS_CHECKER + "=all\n"
                + USESYSTEMCLASSLOADER + "=true\n"
                + PROVIDER_CONFIGURATION + "=abc.MyProvider" );

        BooterDeserializer deserializer = new BooterDeserializer( is );

        assertThat( deserializer.getStartupConfiguration().getProcessChecker() )
                .isEqualTo( ALL );

        assertThat( deserializer.getStartupConfiguration().getClassLoaderConfiguration().isUseSystemClassLoader() )
                .isTrue();

        assertThat( deserializer.getStartupConfiguration().getProviderClassName() )
                .isEqualTo( "abc.MyProvider" );
    }
}
