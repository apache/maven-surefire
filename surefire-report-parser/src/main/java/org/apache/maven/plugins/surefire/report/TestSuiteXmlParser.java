package org.apache.maven.plugins.surefire.report;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.shared.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public final class TestSuiteXmlParser
    extends DefaultHandler
{
    private final NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    private ReportTestSuite defaultSuite;

    private ReportTestSuite currentSuite;

    private Map<String, Integer> classesToSuitesIndex;

    private List<ReportTestSuite> suites;

    private StringBuilder currentElement;

    private ReportTestCase testCase;

    private boolean valid;

    public List<ReportTestSuite> parse( String xmlPath )
        throws ParserConfigurationException, SAXException, IOException
    {
        FileInputStream fileInputStream = new FileInputStream( new File( xmlPath ) );
        InputStreamReader  inputStreamReader = new InputStreamReader( fileInputStream, "UTF-8" );

        try
        {
            return parse( inputStreamReader );
        }
        finally
        {
            inputStreamReader.close();
            fileInputStream.close();
        }
    }

    public List<ReportTestSuite> parse( InputStreamReader stream )
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser saxParser = factory.newSAXParser();

        valid = true;

        classesToSuitesIndex = new HashMap<String, Integer>();
        suites = new ArrayList<ReportTestSuite>();

        saxParser.parse( new InputSource( stream ), this );

        if ( currentSuite != defaultSuite )
        { // omit the defaultSuite if it's empty and there are alternatives
            if ( defaultSuite.getNumberOfTests() == 0 )
            {
                suites.remove( classesToSuitesIndex.get( defaultSuite.getFullClassName() ).intValue() );
            }
        }

        return suites;
    }

    /**
     * {@inheritDoc}
     */
    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        if ( valid )
        {
            try
            {
                if ( "testsuite".equals( qName ) )
                {
                    defaultSuite = new ReportTestSuite();
                    currentSuite = defaultSuite;

                    try
                    {
                        Number time = numberFormat.parse( attributes.getValue( "time" ) );

                        defaultSuite.setTimeElapsed( time.floatValue() );
                    }
                    catch ( NullPointerException e )
                    {
                        System.err.println( "WARNING: no time attribute found on testsuite element" );
                    }

                    final String name = attributes.getValue( "name" );
                    final String group = attributes.getValue( "group" );
                    defaultSuite.setFullClassName( StringUtils.isBlank( group )
                                                       ? /*name is full class name*/ name
                                                       : /*group is package name*/ group + "." + name );

                    suites.add( defaultSuite );
                    classesToSuitesIndex.put( defaultSuite.getFullClassName(), suites.size() - 1 );
                }
                else if ( "testcase".equals( qName ) )
                {
                    currentElement = new StringBuilder();

                    testCase = new ReportTestCase()
                        .setName( attributes.getValue( "name" ) );

                    String fullClassName = attributes.getValue( "classname" );

                    // if the testcase declares its own classname, it may need to belong to its own suite
                    if ( fullClassName != null )
                    {
                        Integer currentSuiteIndex = classesToSuitesIndex.get( fullClassName );
                        if ( currentSuiteIndex == null )
                        {
                            currentSuite = new ReportTestSuite()
                                .setFullClassName( fullClassName );
                            suites.add( currentSuite );
                            classesToSuitesIndex.put( fullClassName, suites.size() - 1 );
                        }
                        else
                        {
                            currentSuite = suites.get( currentSuiteIndex );
                        }
                    }

                    final String timeAsString = attributes.getValue( "time" );
                    final Number time = StringUtils.isBlank( timeAsString ) ? 0 : numberFormat.parse( timeAsString );

                    testCase.setFullClassName( currentSuite.getFullClassName() )
                        .setClassName( currentSuite.getName() )
                        .setFullName( currentSuite.getFullClassName() + "." + testCase.getName() )
                        .setTime( time.floatValue() );

                    if ( currentSuite != defaultSuite )
                    {
                        currentSuite.setTimeElapsed( testCase.getTime() + currentSuite.getTimeElapsed() );
                    }
                }
                else if ( "failure".equals( qName ) )
                {
                    testCase.setFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
                    currentSuite.incrementNumberOfFailures();
                }
                else if ( "error".equals( qName ) )
                {
                    testCase.setFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
                    currentSuite.incrementNumberOfErrors();
                }
                else if ( "skipped".equals( qName ) )
                {
                    final String message = attributes.getValue( "message" );
                    testCase.setFailure( message != null ? message : "skipped", "skipped" );
                    currentSuite.incrementNumberOfSkipped();
                }
                else if ( "flakyFailure".equals( qName ) || "flakyError".equals( qName ) )
                {
                    currentSuite.incrementNumberOfFlakes();
                }
                else if ( "failsafe-summary".equals( qName ) )
                {
                    valid = false;
                }
            }
            catch ( ParseException e )
            {
                throw new SAXException( e.getMessage(), e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( "testcase".equals( qName ) )
        {
            currentSuite.getTestCases().add( testCase );
        }
        else if ( "failure".equals( qName ) || "error".equals( qName ) )
        {
            testCase.setFailureDetail( currentElement.toString() )
                .setFailureErrorLine( parseErrorLine( currentElement, testCase.getFullClassName() ) );
        }
        else if ( "time".equals( qName ) )
        {
            try
            {
                defaultSuite.setTimeElapsed( numberFormat.parse( currentElement.toString() ).floatValue() );
            }
            catch ( ParseException e )
            {
                throw new SAXException( e.getMessage(), e );
            }
        }
        // TODO extract real skipped reasons
    }

    /**
     * {@inheritDoc}
     */
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        assert start >= 0;
        assert length >= 0;
        if ( valid && isNotBlank( start, length, ch ) )
        {
            currentElement.append( ch, start, length );
        }
    }

    public boolean isValid()
    {
        return valid;
    }

    static boolean isNotBlank( int from, int len, char... s )
    {
        assert from >= 0;
        assert len >= 0;
        if ( s != null )
        {
            for ( int i = 0; i < len; i++ )
            {
                char c = s[from++];
                if ( c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' )
                {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isNumeric( StringBuilder s, final int from, final int to )
    {
        assert from >= 0;
        assert from <= to;
        for ( int i = from; i != to; )
        {
            if ( !Character.isDigit( s.charAt( i++ ) ) )
            {
                return false;
            }
        }
        return from != to;
    }

    static String parseErrorLine( StringBuilder currentElement, String fullClassName )
    {
        final String[] linePatterns = { "at " + fullClassName + '.', "at " + fullClassName + '$' };
        int[] indexes = lastIndexOf( currentElement, linePatterns );
        int patternStartsAt = indexes[0];
        if ( patternStartsAt != -1 )
        {
            int searchFrom = patternStartsAt + ( linePatterns[ indexes[1] ] ).length();
            searchFrom = 1 + currentElement.indexOf( ":", searchFrom );
            int searchTo = currentElement.indexOf( ")", searchFrom );
            return isNumeric( currentElement, searchFrom, searchTo )
                ? currentElement.substring( searchFrom, searchTo )
                : "";
        }
        return "";
    }

    static int[] lastIndexOf( StringBuilder source, String... linePatterns )
    {
        int end = source.indexOf( "Caused by:" );
        if ( end == -1 )
        {
            end = source.length();
        }
        int startsAt = -1;
        int pattern = -1;
        for ( int i = 0; i < linePatterns.length; i++ )
        {
            String linePattern = linePatterns[i];
            int currentStartsAt = source.lastIndexOf( linePattern, end );
            if ( currentStartsAt > startsAt )
            {
                startsAt = currentStartsAt;
                pattern = i;
            }
        }
        return new int[] { startsAt, pattern };
    }
}
