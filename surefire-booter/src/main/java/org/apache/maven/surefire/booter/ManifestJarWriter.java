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


import java.io.*;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Constructs a jar containing only a META-INF/MANIFEST.MF with specified attributes.
 */
public class ManifestJarWriter
{

    /**
     * The max length of a line in a Manifest
     */
    public static final int MAX_LINE_LENGTH = 72;

    /**
     * Max length of a line section which is continued. Need to allow
     * for the CRLF.
     */
    public static final int MAX_SECTION_LENGTH = MAX_LINE_LENGTH - 2;

    /**
     * The End-Of-Line marker in manifests
     */
    public static final String EOL = "\r\n";

    private PrintWriter writer;

    public ManifestJarWriter( File jarFile )
        throws IOException
    {

        FileOutputStream fos = new FileOutputStream( jarFile );
        ZipOutputStream zos = new ZipOutputStream( fos );
        zos.setLevel( ZipOutputStream.STORED );
        ZipEntry ze;
        ze = new ZipEntry( "META-INF/MANIFEST.MF" );
        zos.putNextEntry( ze );
        writer = new PrintWriter( zos );
    }

    /**
     * Write a single attribute value out.  Should handle multiple lines of attribute value.
     *
     * @param name  the attribute name, e.g. "Main-Class"
     * @param value the attribute value
     * @throws java.io.IOException if the attribute value cannot be written
     */
    public void writeValue( String name, String value )
        throws IOException
    {
        String nameValue = name + ": " + value;

        StringTokenizer tokenizer = new StringTokenizer( nameValue, "\n\r" );

        String prefix = "";

        while ( tokenizer.hasMoreTokens() )
        {
            writeLine( prefix + tokenizer.nextToken() );
            prefix = " ";
        }
    }

    /**
     * Write a single Manifest line. Should handle more than 72 characters of line
     *
     * @param line the manifest line to be written
     * @throws java.io.IOException if the attribute line cannot be written
     */
    private void writeLine( String line )
        throws IOException
    {
        while ( line.getBytes().length > MAX_LINE_LENGTH )
        {
            // try to find a MAX_LINE_LENGTH byte section
            int breakIndex = MAX_SECTION_LENGTH;
            String section = line.substring( 0, breakIndex );
            while ( section.getBytes().length > MAX_SECTION_LENGTH && breakIndex > 0 )
            {
                breakIndex--;
                section = line.substring( 0, breakIndex );
            }
            if ( breakIndex == 0 )
            {
                throw new IOException( "Unable to write manifest line " + line );
            }
            writer.print( section + EOL );
            line = " " + line.substring( breakIndex );
        }
        writer.print( line + EOL );
    }

    public void close()
    {
        writer.flush();
        writer.close();
    }

}
