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

import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

/**
 * TestNG configurator for 5.3+ versions. TestNG exposes a {@link org.testng.TestNG#configure(java.util.Map)} method.
 * All suppported TestNG options are passed in String format, except
 * <code>TestNGCommandLineArgs.LISTENER_COMMAND_OPT</code> which is <code>List&gt;Class&lt;</code>,
 * <code>TestNGCommandLineArgs.JUNIT_DEF_OPT</code> which is a <code>Boolean</code>,
 * <code>TestNGCommandLineArgs.SKIP_FAILED_INVOCATION_COUNT_OPT</code> which is a <code>Boolean</code>,
 * <code>TestNGCommandLineArgs.OBJECT_FACTORY_COMMAND_OPT</code> which is a <code>Class</code>,
 * <code>TestNGCommandLineArgs.REPORTERS_LIST</code> which is a <code>List&gt;ReporterConfig&lt;</code>.
 * <p/>
 * Test classes and/or suite files are not passed along as options parameters, but configured separately.
 *
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGMapConfigurator
    implements Configurator
{
    public void configure( TestNG testng, Map<String, String> options )
        throws TestSetFailedException
    {
        Map convertedOptions = getConvertedOptions( options );
        testng.configure( convertedOptions );
    }

    public void configure( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException
    {
        String threadCountString = options.get( ProviderParameterNames.THREADCOUNT_PROP );
        int threadCount = ( null != threadCountString ) ? Integer.parseInt( threadCountString ) : 1;
        suite.setThreadCount( threadCount );

        String parallel = options.get( ProviderParameterNames.PARALLEL_PROP );
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
            if ( "listener".equals( key ) )
            {
                val = AbstractDirectConfigurator.loadListenerClasses( entry.getValue() );
            }
            else if ( "objectfactory".equals( key ) )
            {
                val = AbstractDirectConfigurator.loadClass( entry.getValue() );
            }
            else if ( "reporter".equals( key ) )
            {
                // TODO support multiple reporters?
                val = convertReporterConfig( val );
                key = "reporterslist";
            }
            else if ( "junit".equals( key ) )
            {
                val = convert( val, Boolean.class );
            }
            else if ( "skipfailedinvocationcounts".equals( key ) )
            {
                val = convert( val, Boolean.class );
            }
            else if ( "mixed".equals( key ) )
            {
                val = convert( val, Boolean.class );
            }
            else if ( "configfailurepolicy".equals( key ) )
            {
                val = convert( val, String.class );
            }
            else if ( "group-by-instances".equals( key ) )
            {
                val = convert( val, Boolean.class );
            }
            else if ( ProviderParameterNames.THREADCOUNT_PROP.equals( key ) )
            {
                val = convert ( val, String.class );
            }
            // TODO objectfactory... not even documented, does it work?
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
    private Object convertReporterConfig( Object val )
    {
        final String reporterConfigClassName = "org.testng.ReporterConfig";
        try
        {
            Class<?> reporterConfig = Class.forName( reporterConfigClassName );
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

    protected Object convert( Object val, Class<?> type )
    {
        if ( val == null )
        {
            return null;
        }
        if ( type.isAssignableFrom( val.getClass() ) )
        {
            return val;
        }

        if ( ( Boolean.class.equals( type ) || boolean.class.equals( type ) ) && String.class.equals( val.getClass() ) )
        {
            return Boolean.valueOf( (String) val );
        }

        if ( String.class.equals( type ) )
        {
            return val.toString();
        }

        return val;
    }
}