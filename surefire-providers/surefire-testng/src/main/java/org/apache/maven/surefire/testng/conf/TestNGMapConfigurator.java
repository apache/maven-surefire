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
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.surefire.testng.conf.AbstractDirectConfigurator;
import org.apache.maven.surefire.testng.conf.Configurator;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;

/**
 * TestNG configurator for 5.3+ versions. TestNG exposes
 * a {@link org.testng.TestNG#configure(java.util.Map)} method.
 * All suppported TestNG options are passed in String format, except
 * <code>TestNGCommandLineArgs.LISTENER_COMMAND_OPT</code> which is <code>List&gt;Class&lt;</code>,
 * <code>TestNGCommandLineArgs.JUNIT_DEF_OPT</code> which is a <code>Boolean</code>,
 * <code>TestNGCommandLineArgs.SKIP_FAILED_INVOCATION_COUNT_OPT</code> which is a <code>Boolean</code>,
 * <code>TestNGCommandLineArgs.OBJECT_FACTORY_COMMAND_OPT</code> which is a <code>Class</code>,
 * <code>TestNGCommandLineArgs.REPORTERS_LIST</code> which is a <code>List&gt;ReporterConfig&lt;</code>.
 *
 * <p/>
 * Test classes and/or suite files are not passed along as options parameters, but
 * configured separately.
 *
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGMapConfigurator
    implements Configurator
{
    public void configure( TestNG testng, Map options ) throws TestSetFailedException
    {
        Map convertedOptions = new HashMap();
        for ( Iterator it = options.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();
            if ( "listener".equals( key ) )
            {
                val = AbstractDirectConfigurator.loadListenerClasses((String) val);
            }
            if ( "objectfactory".equals( key ) )
            {
                val = AbstractDirectConfigurator.loadClass((String) val);
            }
            if ( "reporter".equals( key ) )
            {
                // TODO support multiple reporters?
                val = convertReporterConfig( val );
                key = "reporterslist";

            }
            if ( "junit".equals( key ) )
            {
                val = convert( val, Boolean.class );
            } else if ( "skipfailedinvocationcounts".equals( key ) )
            {
                val = convert( val, Boolean.class );
            }
            else if ( "threadcount".equals( key ) )
            {
                val = convert( val, String.class );
            }
            // TODO objectfactory... not even documented, does it work?
            if ( key.startsWith("-") )
            {
              convertedOptions.put( key, val );
            }
            else
            {
              convertedOptions.put( "-" + key, val );
            }
        }

        testng.configure( convertedOptions );

    }

    // ReporterConfig only became available in later versions of TestNG
    private Object convertReporterConfig( Object val )
    {
        final String reporterConfigClassName = "org.testng.ReporterConfig";
        try
        {
            Class reporterConfig = Class.forName( reporterConfigClassName );
            Method deserialize = reporterConfig.getMethod( "deserialize", new Class[] { String.class } );
            Object rc = deserialize.invoke( null, new Object[] { val } );
            ArrayList reportersList = new ArrayList();
            reportersList.add( rc );
            return reportersList;
        }
        catch ( Exception e )
        {
            return val;
        }
    }

    protected Object convert( Object val, Class type )
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