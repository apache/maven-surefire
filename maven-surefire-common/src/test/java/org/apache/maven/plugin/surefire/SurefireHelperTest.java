package org.apache.maven.plugin.surefire;

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

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Test of {@link SurefireHelper}.
 */
public class SurefireHelperTest
{
    @Test
    public void shouldBeThreeDumpFiles()
    {
        String[] dumps = SurefireHelper.getDumpFilesToPrint();
        assertThat( dumps ).hasSize( 3 );
        assertThat( dumps ).doesNotHaveDuplicates();
        List<String> onlyStrings = new ArrayList<String>();
        addAll( onlyStrings, dumps );
        onlyStrings.removeAll( singleton( (String) null ) );
        assertThat( onlyStrings ).hasSize( 3 );
    }

    @Test
    public void shouldCloneDumpFiles()
    {
        String[] dumps1 = SurefireHelper.getDumpFilesToPrint();
        String[] dumps2 = SurefireHelper.getDumpFilesToPrint();
        assertThat( dumps1 ).isNotSameAs( dumps2 );
    }

    @Test
    public void testConstants()
    {
        assertThat( SurefireHelper.DUMPSTREAM_FILENAME_FORMATTER )
                .isEqualTo( SurefireHelper.DUMP_FILE_PREFIX + "%d.dumpstream" );

        assertThat( String.format( SurefireHelper.DUMPSTREAM_FILENAME_FORMATTER, 5) )
                .endsWith( "-jvmRun5.dumpstream" );
    }

    @Test
    public void shouldEscapeWindowsPath()
    {
        assumeTrue( IS_OS_WINDOWS );
        String root = "X:\\path\\to\\project\\";
        String pathToJar = "target\\surefire\\surefirebooter4942721306300108667.jar";
        int projectNameLength = 247 - root.length() - pathToJar.length();
        StringBuilder projectFolder = new StringBuilder();
        for ( int i = 0; i < projectNameLength; i++ )
        {
            projectFolder.append( 'x' );
        }
        String path = root + projectFolder + "\\" + pathToJar;
        String escaped = escapeToPlatformPath( path );
        assertThat( escaped ).isEqualTo( "\\\\?\\" + path );

        path = root + "\\" + pathToJar;
        escaped = escapeToPlatformPath( path );
        assertThat( escaped ).isEqualTo( root + "\\" + pathToJar );
    }
}
