package cwd;

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

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;

public class CurrentWorkingDirectoryInForkedModeTest
{

    @Test
    public void testCurrentWorkingDirectoryPropagation()
        throws Exception
    {

        File projectDirectory = new File( System.getProperty( "maven.project.base.directory" ) );
        File forkDirectory = new File( projectDirectory, "cwd_1" );
        forkDirectory.deleteOnExit();

        assertThat( System.getProperty( "basedir" ) ).isEqualTo( projectDirectory.getCanonicalPath() );
        assertThat( System.getProperty( "user.dir" ) ).isEqualTo( forkDirectory.getCanonicalPath() );
        assertThat( new File( "." ).getCanonicalPath() ).isEqualTo( forkDirectory.getCanonicalPath() );

    }

}
