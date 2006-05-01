package org.apache.maven.surefire.util;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedList;

public class PrettyPrintXMLWriter
    implements XMLWriter
{
    private PrintWriter writer;

    private LinkedList elementStack = new LinkedList();

    private boolean tagInProgress;

    private int depth;

    private String lineIndenter;

    private String encoding;

    private String docType;

    private boolean readyForNewLine;

    private boolean tagIsEmpty;

    public PrettyPrintXMLWriter( PrintWriter writer, String lineIndenter )
    {
        this( writer, lineIndenter, null, null );
    }

    public PrettyPrintXMLWriter( Writer writer, String lineIndenter )
    {
        this( new PrintWriter( writer ), lineIndenter );
    }

    public PrettyPrintXMLWriter( PrintWriter writer )
    {
        this( writer, null, null );
    }

    public PrettyPrintXMLWriter( Writer writer )
    {
        this( new PrintWriter( writer ) );
    }

    public PrettyPrintXMLWriter( PrintWriter writer, String lineIndenter, String encoding, String doctype )
    {
        this.writer = writer;

        this.lineIndenter = lineIndenter;

        this.encoding = encoding;

        this.docType = doctype;

        if ( docType != null || encoding != null )
        {
            writeDocumentHeaders();
        }
    }

    public PrettyPrintXMLWriter( Writer writer, String lineIndenter, String encoding, String doctype )
    {
        this( new PrintWriter( writer ), lineIndenter, encoding, doctype );
    }

    public PrettyPrintXMLWriter( PrintWriter writer, String encoding, String doctype )
    {
        this( writer, "  ", encoding, doctype );
    }

    public PrettyPrintXMLWriter( Writer writer, String encoding, String doctype )
    {
        this( new PrintWriter( writer ), encoding, doctype );
    }

    public void startElement( String name )
    {
        tagIsEmpty = false;

        finishTag();

        write( "<" );

        write( name );

        elementStack.addLast( name );

        tagInProgress = true;

        depth++;

        readyForNewLine = true;

        tagIsEmpty = true;
    }

    public void writeText( String text )
    {
        writeText( text, true );
    }

    public void writeMarkup( String text )
    {
        writeText( text, false );
    }

    private void writeText( String text, boolean escapeXml )
    {
        readyForNewLine = false;

        tagIsEmpty = false;

        finishTag();

        if ( escapeXml )
        {
            text = escapeXml( text );
        }

        write( text );
    }

    private static String escapeXml( String text )
    {
        text = StringUtils.replace( text, "&", "&amp;" );
        text = StringUtils.replace( text, "<", "&amp;" );
        text = StringUtils.replace( text, ">", "&amp;" );
        text = StringUtils.replace( text, "\"", "&quot;" );
        text = StringUtils.replace( text, "\'", "&apos;" );

        return text;
    }

    public void addAttribute( String key, String value )
    {
        write( " " );

        write( key );

        write( "=\"" );

        write( escapeXml( value ) );

        write( "\"" );
    }

    public void endElement()
    {
        depth--;

        if ( tagIsEmpty )
        {
            write( "/" );

            readyForNewLine = false;

            finishTag();

            elementStack.removeLast();
        }
        else
        {
            finishTag();

            write( "</" + elementStack.removeLast() + ">" );
        }

        readyForNewLine = true;
    }

    private void write( String str )
    {
        writer.write( str );
    }

    private void finishTag()
    {
        if ( tagInProgress )
        {
            write( ">" );
        }

        tagInProgress = false;

        if ( readyForNewLine )
        {
            endOfLine();
        }
        readyForNewLine = false;

        tagIsEmpty = false;
    }

    protected void endOfLine()
    {
        write( "\n" );

        for ( int i = 0; i < depth; i++ )
        {
            write( lineIndenter );
        }
    }

    private void writeDocumentHeaders()
    {
        write( "<?xml version=\"1.0\"" );

        if ( encoding != null )
        {
            write( " encoding=\"" + encoding + "\"" );
        }

        write( "?>" );

        endOfLine();

        if ( docType != null )
        {
            write( "<!DOCTYPE " );

            write( docType );

            write( ">" );

            endOfLine();
        }
    }
}
