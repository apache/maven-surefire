package org.apache.maven.plugin.surefire.report;

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

import java.io.PrintWriter;
import java.util.LinkedList;

import org.apache.maven.shared.utils.xml.XMLWriter;

public class PrettyPrintXMLWriter
    implements XMLWriter
{
    private final PrintWriter writer;

    private final LinkedList<String> elementStack = new LinkedList<String>();

    private boolean tagInProgress;

    private int depth;

    private final String lineIndenter;

    private final String encoding;

    private final String docType;

    private boolean readyForNewLine;

    private boolean tagIsEmpty;

    public PrettyPrintXMLWriter( PrintWriter writer )
    {
        this( writer, null, null );
    }

    private PrettyPrintXMLWriter( PrintWriter writer, String lineIndenter, String encoding, String doctype )
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

    public void setEncoding( String encoding )
    {
        throw new RuntimeException( "Not Implemented" );
    }

    public void setDocType( String docType )
    {
        throw new RuntimeException( "Not Implemented" );
    }

    private PrettyPrintXMLWriter( PrintWriter writer, String encoding, String doctype )
    {
        this( writer, "  ", encoding, doctype );
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
        StringBuffer sb = new StringBuffer( text.length() * 2 );
        for ( int i = 0; i < text.length(); i++ )
        {
            char c = text.charAt( i );
            if ( c < 32 )
            {
                if ( c == '\n' || c == '\r' || c == '\t' )
                {
                    sb.append( c );
                }
                else
                {
                    // uh-oh!  This character is illegal in XML 1.0!
                    // http://www.w3.org/TR/1998/REC-xml-19980210#charsets
                    // we're going to deliberately doubly-XML escape it...
                    // there's nothing better we can do! :-(
                    // SUREFIRE-456
                    sb.append( "&amp;#" ).append( (int) c ).append( ';' );
                }
            }
            else if ( c == '<' )
            {
                sb.append( "&lt;" );
            }
            else if ( c == '>' )
            {
                sb.append( "&gt;" );
            }
            else if ( c == '&' )
            {
                sb.append( "&amp;" );
            }
            else if ( c == '"' )
            {
                sb.append( "&quot;" );
            }
            else if ( c == '\'' )
            {
                sb.append( "&apos;" );
            }
            else
            {
                sb.append( c );
            }
        }
        return sb.toString();
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
