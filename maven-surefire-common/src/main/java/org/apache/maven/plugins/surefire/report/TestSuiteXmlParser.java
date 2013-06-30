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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class TestSuiteXmlParser
    extends DefaultHandler
{
    private ReportTestSuite defaultSuite;

    private ReportTestSuite currentSuite;

    private Map<String, ReportTestSuite> classesToSuites;

    private final NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    /**
     * @noinspection StringBufferField
     */
    private StringBuffer currentElement;

    private ReportTestCase testCase;

    private boolean valid;

    public Collection<ReportTestSuite> parse( String xmlPath )
        throws ParserConfigurationException, SAXException, IOException
    {

        File f = new File( xmlPath );

        FileInputStream fileInputStream = new FileInputStream( f );

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

    public Collection<ReportTestSuite> parse( InputStreamReader stream )
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser saxParser = factory.newSAXParser();

        valid = true;

        classesToSuites = new HashMap<String, ReportTestSuite>();

        saxParser.parse( new InputSource( stream ), this );

        if ( currentSuite != defaultSuite )
        { // omit the defaultSuite if it's empty and there are alternatives
            if ( defaultSuite.getNumberOfTests() == 0 )
            {
                classesToSuites.remove( defaultSuite.getFullClassName() );
            }
        }

        return classesToSuites.values();
    }

    /**
     * {@inheritDoc}
     */
    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        if ( !valid )
        {
            return;
        }
        try
        {
            if ( "testsuite".equals( qName ) )
            {
                currentSuite = defaultSuite = new ReportTestSuite();

                try
                {
                    Number time = numberFormat.parse( attributes.getValue( "time" ) );

                    defaultSuite.setTimeElapsed( time.floatValue() );
                }
                catch ( NullPointerException npe )
                {
                    System.err.println( "WARNING: no time attribute found on testsuite element" );
                }

                //check if group attribute is existing
                if ( attributes.getValue( "group" ) != null && !"".equals( attributes.getValue( "group" ) ) )
                {
                    String packageName = attributes.getValue( "group" );
                    String name = attributes.getValue( "name" );

                    defaultSuite.setFullClassName( packageName + "." + name );
                }
                else
                {
                    String fullClassName = attributes.getValue( "name" );
                    defaultSuite.setFullClassName( fullClassName );
                }

                classesToSuites.put( defaultSuite.getFullClassName(), defaultSuite );
            }
            else if ( "testcase".equals( qName ) )
            {
                currentElement = new StringBuffer();

                testCase = new ReportTestCase();

                testCase.setName( attributes.getValue( "name" ) );

                String fullClassName = attributes.getValue( "classname" );

                // if the testcase declares its own classname, it may need to belong to its own suite
                if ( fullClassName != null )
                {
                    currentSuite = classesToSuites.get( fullClassName );
                    if ( currentSuite == null )
                    {
                        currentSuite = new ReportTestSuite();
                        currentSuite.setFullClassName( fullClassName );
                        classesToSuites.put( fullClassName, currentSuite );
                    }
                }

                testCase.setFullClassName( currentSuite.getFullClassName() );
                testCase.setClassName( currentSuite.getName() );
                testCase.setFullName( currentSuite.getFullClassName() + "." + testCase.getName() );

                String timeAsString = attributes.getValue( "time" );

                Number time = 0;

                if ( timeAsString != null )
                {
                    time = numberFormat.parse( timeAsString );
                }

                testCase.setTime( time.floatValue() );

                if ( currentSuite != defaultSuite )
                {
                    currentSuite.setTimeElapsed( time.floatValue() + currentSuite.getTimeElapsed() );
                }
            }
            else if ( "failure".equals( qName ) )
            {
                testCase.addFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
                currentSuite.setNumberOfFailures( 1 + currentSuite.getNumberOfFailures() );
            }
            else if ( "error".equals( qName ) )
            {
                testCase.addFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
                currentSuite.setNumberOfErrors( 1 + currentSuite.getNumberOfErrors() );
            }
            else if ( "skipped".equals( qName ) )
            {
                final String message = attributes.getValue( "message" );
                testCase.addFailure( message != null ? message : "skipped", "skipped" );
                currentSuite.setNumberOfSkipped( 1 + currentSuite.getNumberOfSkipped() );
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
        else if ( "failure".equals( qName ) )
        {
            Map<String, Object> failure = testCase.getFailure();

            failure.put( "detail", parseCause( currentElement.toString() ) );
        }
        else if ( "error".equals( qName ) )
        {
            Map<String, Object> error = testCase.getFailure();

            error.put( "detail", parseCause( currentElement.toString() ) );
        }
        else if ( "time".equals( qName ) )
        {
            try
            {
                Number time = numberFormat.parse( currentElement.toString() );
                defaultSuite.setTimeElapsed( time.floatValue() );
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
        if ( !valid )
        {
            return;
        }
        String s = new String( ch, start, length );

        if ( !"".equals( s.trim() ) )
        {
            currentElement.append( s );
        }
    }

    private List<String> parseCause( String detail )
    {
        String fullName = testCase.getFullName();
        String name = fullName.substring( fullName.lastIndexOf( "." ) + 1 );
        return parseCause( detail, name );
    }

    private List<String> parseCause( String detail, String compareTo )
    {
        StringTokenizer stringTokenizer = new StringTokenizer( detail, "\n" );
        List<String> parsedDetail = new ArrayList<String>( stringTokenizer.countTokens() );

        while ( stringTokenizer.hasMoreTokens() )
        {
            String lineString = stringTokenizer.nextToken().trim();
            parsedDetail.add( lineString );
            if ( lineString.contains( compareTo ) )
            {
                break;
            }
        }

        return parsedDetail;
    }

    public boolean isValid()
    {
        return valid;
    }
}
