package org.apache.maven.surefire.util.internal;

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

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test of {@link SystemUtils}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class SystemUtilsTest
{
    @Test
    public void shouldBeJava9()
    {
        String simpleVersion = "9";
        double version = SystemUtils.extractJavaSpecVersion( simpleVersion );
        assertThat( version ).isEqualTo( 9d );
    }

    @Test
    public void shouldBeJava8()
    {
        String simpleVersion = "1.8";
        double version = SystemUtils.extractJavaSpecVersion( simpleVersion );
        assertThat( version ).isEqualTo( 1.8d );
    }

    @Test
    public void shouldBeJavaMajorAndMinor()
    {
        String simpleVersion = "12.345.6";
        double version = SystemUtils.extractJavaSpecVersion( simpleVersion );
        assertThat( version ).isEqualTo( 12.345d );
    }

    @Test
    public void shouldBeCurrentJavaVersion()
    {
        Double parsedVersion = SystemUtils.javaSpecVersion();
        String expectedVersion = System.getProperty( "java.specification.version" );
        assertThat( parsedVersion.toString() ).isEqualTo( expectedVersion );
    }

    @Test
    public void shouldBePlatformClassLoader()
    {
        ClassLoader cl = SystemUtils.platformClassLoader();
        if ( SystemUtils.javaSpecVersion() < 9 )
        {
            assertThat( cl ).isNull();
        }
        else
        {
            assertThat( cl ).isNotNull();
        }
    }

    @Test
    public void shouldNotFindClassLoader()
    {
        ClassLoader cl = SystemUtils.reflectClassLoader( getClass(), "_getPlatformClassLoader_" );
        assertThat( cl ).isNull();
    }

    @Test
    public void shouldFindClassLoader()
    {
        ClassLoader cl = SystemUtils.reflectClassLoader( getClass(), "getPlatformClassLoader" );
        assertThat( cl ).isSameAs( ClassLoader.getSystemClassLoader() );
    }

    public static ClassLoader getPlatformClassLoader()
    {
        return ClassLoader.getSystemClassLoader();
    }
}
