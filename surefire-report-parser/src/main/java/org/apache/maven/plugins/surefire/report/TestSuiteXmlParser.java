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
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.shared.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

/**
 *
 */
public final class TestSuiteXmlParser
    extends DefaultHandler
{
    private final NumberFormat numberFormat = NumberFormat.getInstance( ENGLISH );

    private final ConsoleLogger consoleLogger;

    private ReportTestSuite defaultSuite;

    private ReportTestSuite currentSuite;

    private Map<String, Integer> classesToSuitesIndex;

    private List<ReportTestSuite> suites;

    private StringBuilder currentElement;

    private ReportTestCase testCase;

    private boolean valid;

    public TestSuiteXmlParser( ConsoleLogger consoleLogger )
    {
        this.consoleLogger = consoleLogger;
    }

    public List<ReportTestSuite> parse( String xmlPath )
        throws ParserConfigurationException, SAXException, IOException
    {
        File f = new File( xmlPath );
        try ( InputStreamReader stream = new InputStreamReader( new FileInputStream( f ), UTF_8 ) )
        {
            return parse( stream );
        }
    }

    public List<ReportTestSuite> parse( InputStreamReader stream )
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser saxParser = factory.newSAXParser();

        valid = true;

        classesToSuitesIndex = new HashMap<>();
        suites = new ArrayList<>();

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
    @Override
    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        if ( valid )
        {
            try
            {
                switch ( qName )
                {
                    case "testsuite":
                        defaultSuite = new ReportTestSuite();
                        currentSuite = defaultSuite;

                        try
                        {
                            Number time = numberFormat.parse( attributes.getValue( "time" ) );

                            defaultSuite.setTimeElapsed( time.floatValue() );
                        }
                        catch ( NullPointerException e )
                        {
                            consoleLogger.error( "WARNING: no time attribute found on testsuite element" );
                        }

                        final String name = attributes.getValue( "name" );
                        final String group = attributes.getValue( "group" );
                        defaultSuite.setFullClassName( StringUtils.isBlank( group )
                                ? /*name is full class name*/ name
                                : /*group is package name*/ group + "." + name );

                        suites.add( defaultSuite );
                        classesToSuitesIndex.put( defaultSuite.getFullClassName(), suites.size() - 1 );
                        break;
                    case "testcase":
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

                        String timeAsString = attributes.getValue( "time" );
                        Number time = StringUtils.isBlank( timeAsString ) ? 0 : numberFormat.parse( timeAsString );

                        testCase.setFullClassName( currentSuite.getFullClassName() )
                                .setClassName( currentSuite.getName() )
                                .setFullName( currentSuite.getFullClassName() + "." + testCase.getName() )
                                .setTime( time.floatValue() );

                        if ( currentSuite != defaultSuite )
                        {
                            currentSuite.setTimeElapsed( testCase.getTime() + currentSuite.getTimeElapsed() );
                        }
                        break;
                    case "failure":
                        testCase.setFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
                        currentSuite.incrementNumberOfFailures();
                        break;
                    case "error":
                        testCase.setError( attributes.getValue( "message" ), attributes.getValue( "type" ) );
                        currentSuite.incrementNumberOfErrors();
                        break;
                    case "skipped":
                        String message = attributes.getValue( "message" );
                        testCase.setSkipped( message != null ? message : "skipped" );
                        currentSuite.incrementNumberOfSkipped();
                        break;
                    case "flakyFailure":
                    case "flakyError":
                        currentSuite.incrementNumberOfFlakes();
                        break;
                    case "failsafe-summary":
                        valid = false;
                        break;
                    default:
                        break;
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
    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        switch ( qName )
        {
            case "testcase":
                currentSuite.getTestCases().add( testCase );
                break;
            case "failure":
            case "error":
                testCase.setFailureDetail( currentElement.toString() )
                        .setFailureErrorLine( parseErrorLine( currentElement, testCase.getFullClassName() ) );
                break;
            case "time":
                try
                {
                    defaultSuite.setTimeElapsed( numberFormat.parse( currentElement.toString() ).floatValue() );
                }
                catch ( ParseException e )
                {
                    throw new SAXException( e.getMessage(), e );
                }
                break;
            default:
                break;
        }
        // TODO extract real skipped reasons
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters( char[] ch, int start, int length )
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
