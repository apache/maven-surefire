package org.apache.maven.plugin.surefire.util;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ScannerUtil}
 */
public class ScannerUtilTest
{

    @Test
    public void shouldConvertJarFileResourceToJavaClassName()
    {
        String className = ScannerUtil.convertJarFileResourceToJavaClassName( "org/apache/pkg/MyService.class" );

        assertThat( className )
                .isEqualTo( "org.apache.pkg.MyService" );

        className = ScannerUtil.convertJarFileResourceToJavaClassName( "META-INF/MANIFEST.MF" );

        assertThat( className )
                .isEqualTo( "META-INF.MANIFEST.MF" );
    }

    @Test
    public void shouldBeClassFile()
    {
        assertThat( ScannerUtil.isJavaClassFile( "META-INF/MANIFEST.MF" ) )
                .isFalse();

        assertThat( ScannerUtil.isJavaClassFile( "org/apache/pkg/MyService.class" ) )
                .isTrue();
    }
}
