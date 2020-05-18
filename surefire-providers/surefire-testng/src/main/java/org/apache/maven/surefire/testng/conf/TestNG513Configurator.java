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

import org.apache.maven.surefire.api.testset.TestSetFailedException;

/**
 * TestNG 5.13 configurator. Changed: "reporterslist" need String instead of List&lt;ReporterConfig&gt;.
 */
public class TestNG513Configurator
    extends TestNG510Configurator
{

    @Override
    protected Object convertReporterConfig( Object val )
    {
        return val;
    }

    @Override
    protected Object convertListeners( String listenerClasses ) throws TestSetFailedException
    {
        return convertListenersString( listenerClasses );
    }

    static String convertListenersString( String listenerClasses )
    {
        if ( listenerClasses == null || listenerClasses.trim().isEmpty() )
        {
            return listenerClasses;
        }

        StringBuilder sb = new StringBuilder();
        String[] classNames = listenerClasses.split( "\\s*,\\s*(\\r?\\n)?\\s*" );
        for ( int i = 0; i < classNames.length; i++ )
        {
            String className = classNames[i];
            sb.append( className );
            if ( i < classNames.length - 1 )
            {
                sb.append( ',' );
            }
        }

        return sb.toString();
    }
}
