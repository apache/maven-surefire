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

import org.testng.TestNG;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TestNG configurator for 5.3+ versions. TestNG exposes
 * a {@link org.testng.TestNG#configure(java.util.Map)} method.
 * All suppported TestNG options are passed in String format, except
 * <code>TestNGCommandLineArgs.LISTENER_COMMAND_OPT</code> which is <code>List<Class></code>
 * and <code>TestNGCommandLineArgs.JUNIT_DEF_OPT</code> which is a <code>Boolean</code>.
 * <p/>
 * Test classes and/or suite files are not passed along as options parameters, but
 * configured separately.
 */
public class TestNGMapConfigurator
    implements Configurator
{
    public void configure( TestNG testng, Map options )
    {
        Map convertedOptions = new HashMap();
        for ( Iterator it = options.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();
            if ( "junit".equals( key ) )
            {
                val = convert( val, Boolean.class );
            }
            else if ( "threadcount".equals( key ) )
            {
                val = convert( val, String.class );
            }
            convertedOptions.put( "-" + key, val );
        }

        testng.configure( convertedOptions );
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