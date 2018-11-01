package org.apache.maven.surefire.testng.conf;

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

import static java.lang.Integer.parseInt;
import static org.apache.maven.surefire.booter.ProviderParameterNames.PARALLEL_PROP;
import static org.apache.maven.surefire.booter.ProviderParameterNames.THREADCOUNT_PROP;
import static org.apache.maven.surefire.testng.conf.AbstractDirectConfigurator.loadListenerClasses;

/**
 * TestNG configurator for 5.3+ versions. TestNG exposes a {@link org.testng.TestNG#configure(java.util.Map)} method.
 * All supported TestNG options are passed in String format, except
 * {@link org.testng.TestNGCommandLineArgs#LISTENER_COMMAND_OPT} which is {@link java.util.List List&gt;Class&lt;},
 * {@link org.testng.TestNGCommandLineArgs#JUNIT_DEF_OPT} which is a {@link Boolean},
 * {@link org.testng.TestNGCommandLineArgs#SKIP_FAILED_INVOCATION_COUNT_OPT} which is a {@link Boolean},
 * {@link org.testng.TestNGCommandLineArgs#OBJECT_FACTORY_COMMAND_OPT} which is a {@link Class},
 * {@link org.testng.TestNGCommandLineArgs#REPORTERS_LIST} which is a {@link java.util.List List&gt;ReporterConfig&lt;}.
 * <br>
 * Test classes and/or suite files are not passed along as options parameters, but configured separately.
 *
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGMapConfigurator
    implements Configurator
{
    @Override
    public void configure( TestNG testng, Map<String, String> options )
        throws TestSetFailedException
    {
        Map convertedOptions = getConvertedOptions( options );
        testng.configure( convertedOptions );
    }

    @Override
    public void configure( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException
    {
        String threadCountAsString = options.get( THREADCOUNT_PROP );
        int threadCount = threadCountAsString == null ? 1 : parseInt( threadCountAsString );
        suite.setThreadCount( threadCount );

        String parallel = options.get( PARALLEL_PROP );
        if ( parallel != null )
        {
            suite.setParallel( parallel );
        }
    }

    Map<String, Object> getConvertedOptions( Map<String, String> options )
        throws TestSetFailedException
    {
        Map<String, Object> convertedOptions = new HashMap<String, Object>();
        convertedOptions.put( "-mixed", false );
        for ( Map.Entry<String, String> entry : options.entrySet() )
        {
            String key = entry.getKey();
            Object val = entry.getValue();
            switch ( key )
            {
                case "listener":
                    val = convertListeners( entry.getValue() );
                    break;
                case "objectfactory":
                case "testrunfactory":
                    val = AbstractDirectConfigurator.loadClass( entry.getValue() );
                    break;
                case "reporter":
                    // for TestNG 5.6 or higher
                    // TODO support multiple reporters?
                    val = convertReporterConfig( val );
                    key = "reporterslist";
                    break;
                case "junit":
                case "skipfailedinvocationcounts":
                case "mixed":
                case "group-by-instances":
                    val = convert( val, Boolean.class );
                    break;
                case "configfailurepolicy":
                case THREADCOUNT_PROP:
                    val = convert( val, String.class );
                    break;
                // for TestNG 6.9.7 or higher
                case "suitethreadpoolsize":
                    // for TestNG 5.10 or higher
                case "dataproviderthreadcount":
                    val = convert( val, Integer.class );
                    break;
                default:
                    break;
            }

            if ( key.startsWith( "-" ) )
            {
                convertedOptions.put( key, val );
            }
            else
            {
                convertedOptions.put( "-" + key, val );
            }
        }
        return convertedOptions;
    }

    // ReporterConfig only became available in later versions of TestNG
    protected Object convertReporterConfig( Object val )
    {
        try
        {
            Class<?> reporterConfig = Class.forName( "org.testng.ReporterConfig" );
            Method deserialize = reporterConfig.getMethod( "deserialize", String.class );
            Object rc = deserialize.invoke( null, val );
            ArrayList<Object> reportersList = new ArrayList<Object>();
            reportersList.add( rc );
            return reportersList;
        }
        catch ( Exception e )
        {
            return val;
        }
    }

    protected Object convertListeners( String listenerClasses ) throws TestSetFailedException
    {
        return loadListenerClasses( listenerClasses );
    }

    protected Object convert( Object val, Class<?> type )
    {
        if ( val == null )
        {
            return null;
        }
        else if ( type.isAssignableFrom( val.getClass() ) )
        {
            return val;
        }
        else if ( ( type == Boolean.class || type == boolean.class ) && val.getClass() == String.class )
        {
            return Boolean.valueOf( (String) val );
        }
        else if ( ( type == Integer.class || type == int.class ) && val.getClass() == String.class )
        {
            return Integer.valueOf( (String) val );
        }
        else if ( type == String.class )
        {
            return val.toString();
        }
        else
        {
            return val;
        }
    }
}