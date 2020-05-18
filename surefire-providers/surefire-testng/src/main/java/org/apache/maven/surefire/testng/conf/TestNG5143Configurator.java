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

import java.util.Map;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

/**
 * TestNG 5.14.3 configurator. Changed: "reporterslist" replaced by "reporter",
 *                                       and "listener" can use String instead of List&lt;Class&gt;.
 */
public class TestNG5143Configurator
    extends TestNG5141Configurator
{

    @Override
    Map<String, Object> getConvertedOptions( Map<String, String> options )
            throws TestSetFailedException
    {
        Map<String, Object> convertedOptions = super.getConvertedOptions( options );
        for ( Map.Entry<String, Object> entry : convertedOptions.entrySet() )
        {
            String key = entry.getKey();
            if ( "-reporterslist".equals( key ) )
            {
                convertedOptions.remove( "-reporterslist" );
                Object value = entry.getValue();
                convertedOptions.put( "-reporter", value );
                break;
            }
        }
        return convertedOptions;
    }


    @Override
    protected Object convertListeners( String listenerClasses ) throws TestSetFailedException
    {
        return convertListenersString( listenerClasses );
    }
}
