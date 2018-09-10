package org.apache.maven.surefire.its.fixture;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;

import junit.framework.Assert;
import org.hamcrest.Matcher;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Kristian Rosenvold
 */
public class TestFile
{
    private final File file;

    private final Charset encoding;

    private final OutputValidator surefireVerifier;

    public TestFile( File file, OutputValidator surefireVerifier )
    {
        this( file, Charset.defaultCharset(), surefireVerifier);
    }

    public TestFile( File file, Charset charset, OutputValidator surefireVerifier )
    {
        try
        {
            this.file = file.getCanonicalFile();
            this.encoding = charset == null ? Charset.defaultCharset() : charset;
            this.surefireVerifier = surefireVerifier;
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( file.getPath() );
        }
    }

    public OutputValidator assertFileExists()
    {
        assertTrue( "File doesn't exist: " + file.getAbsolutePath(), file.exists() );
        return surefireVerifier;
    }

    public OutputValidator assertFileNotExists()
    {
        assertTrue( "File doesn't exist: " + file.getAbsolutePath(), !file.exists() );
        return surefireVerifier;
    }

    public void delete()
    {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public String getAbsolutePath()
    {
        return file.getAbsolutePath();
    }

    public boolean exists()
    {
        return file.exists();
    }

    public FileInputStream getFileInputStream()
        throws FileNotFoundException
    {
        return new FileInputStream( file );
    }

    public String slurpFile()
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader;
            reader = new BufferedReader( new FileReader( file ) );
            for ( String line = reader.readLine(); line != null; line = reader.readLine() )
            {
                sb.append( line );
            }
            reader.close();
            return sb.toString();
        }
        catch ( IOException e )
        {
            throw new SurefireVerifierException( e );
        }

    }

    public String readFileToString()
    {
        try
        {
            return FileUtils.readFileToString( file );
        }
        catch ( IOException e )
        {
            throw new SurefireVerifierException( e );
        }
    }

    public boolean isFile()
    {
        return file.isFile();
    }

    public TestFile assertContainsText( Matcher<String> matcher )
    {
        final List<String> list = surefireVerifier.loadFile( file, encoding );
        for ( String line : list )
        {
            if ( matcher.matches( line ) )
            {
                return this;
            }
        }
        Assert.fail( "Did not find expected message in log" );
        return null;
    }

    public TestFile assertContainsText( String text )
    {
        return assertContainsText( containsString( text ) );
    }

    public URI toURI()
    {
        return file.toURI();
    }

    public File getFile()
    {
        return file;
    }
}
