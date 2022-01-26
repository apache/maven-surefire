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

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.shared.utils.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static java.io.File.pathSeparator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests isolated CL.
 */
public class IsolatedClassLoaderTest
{
    private IsolatedClassLoader classLoader;

    @Before
    public void prepareClassLoader() throws Exception
    {
        classLoader = new IsolatedClassLoader( null, false, "role" );

        String[] files = FileUtils.fileRead( new File( "target/test-classpath/cp.txt" ), "UTF-8" )
                .split( pathSeparator );

        for ( String file : files )
        {
            URL fileUrl = new File( file ).toURI().toURL();
            classLoader.addURL( fileUrl );
        }
    }

    @Test
    public void shouldLoadIsolatedClass() throws Exception
    {
        Class<?> isolatedClass = classLoader.loadClass( AbstractProvider.class.getName() );
        assertThat( isolatedClass, is( notNullValue() ) );
        assertThat( isolatedClass.getName(), is( AbstractProvider.class.getName() ) );
        assertThat( isolatedClass, is( not( (Class) AbstractProvider.class ) ) );
    }
}
