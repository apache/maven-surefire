package org.apache.maven.surefire.api.util;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Unit test for the surefire temp file manager.
 *
 * @author Markus Spann
 */
public class TempFileManagerTest extends TestCase
{

    @Test
    public void testDefaultInstance()
    {
        TempFileManager tfm = TempFileManager.instance();
        assertSame( tfm, TempFileManager.instance( (File) null ) );
        assertSame( tfm, TempFileManager.instance( (String) null ) );
        assertSame( tfm, TempFileManager.instance( new File( System.getProperty( "java.io.tmpdir" ) ) ) );

        assertThat( tfm.getTempDir() ).isEqualTo( new File( System.getProperty( "java.io.tmpdir" ) ) );
        assertThat( tfm.toString() ).contains( "tempDir=" + tfm.getTempDir().getPath() );
        assertThat( tfm.toString() ).contains( "baseName=" + getInternalState( tfm, "baseName" ) );
    }

    @Test
    public void testSubdirInstance()
    {
        String subDirName = TempFileManagerTest.class.getSimpleName() + new Random().nextLong();
        TempFileManager tfm = TempFileManager.instance( subDirName );
        assertEquals( tfm.getTempDir(), new File( System.getProperty( "java.io.tmpdir" ), subDirName ) );
        assertFalse( tfm.getTempDir() + " should not exist", tfm.getTempDir().exists() );
    }

    @Test
    public void testCreateTempFileAndDelete()
    {
        String subDirName = TempFileManagerTest.class.getSimpleName() + new Random().nextLong();
        TempFileManager tfm = TempFileManager.instance( subDirName );
        String prefix = "prefix";
        String suffix = "suffix";
        File tempFile = tfm.createTempFile( prefix, suffix );
        assertThat( tempFile ).exists();
        assertThat( tempFile ).isWritable();
        assertTrue( tempFile.getParentFile().equals( tfm.getTempDir() ) );
        assertThat( tempFile.getName() ).startsWith( prefix );
        assertThat( tempFile.getName() ).endsWith( suffix );
        assertThat( tempFile.getName() ).contains( (String) getInternalState( tfm, "baseName" ) );

        List<File> tempFiles = getInternalState( tfm, "tempFiles" );
        assertThat( tempFiles ).contains( tempFile );
        assertThat( tempFiles ).size().isEqualTo( 1 );

        tfm.deleteAll();
        assertThat( tempFile ).doesNotExist();
        assertThat( tfm.getTempDir() ).doesNotExist();
    }

    @Test
    public void testFileCreateTempFile() throws IOException
    {
        File tempFile = File.createTempFile( "TempFileManager", ".tmp" );
        assertTrue( tempFile.exists() );
        assertTrue( tempFile.delete() );
        assertFalse( tempFile.exists() );
    }

    @Test
    public void testDeleteOnExit()
    {
        TempFileManager tfm = TempFileManager.instance();

        assertFalse( tfm.isDeleteOnExit() );
        tfm.setDeleteOnExit( true );
        assertTrue( tfm.isDeleteOnExit() );
    }


}
