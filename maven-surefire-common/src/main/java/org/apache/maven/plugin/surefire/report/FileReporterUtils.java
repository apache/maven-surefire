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

/**
 * Utils class for file-based reporters
 * 
 * @author Andreas Gudian
 */
public class FileReporterUtils
{
    public static String stripIllegalFilenameChars( String original )
    {
        String result = original;
        String illegalChars = getOSSpecificIllegalChars();
        for ( int i = 0; i < illegalChars.length(); i++ )
            result = result.replace( illegalChars.charAt( i ), '_' );

        return result;
    }

    private static String getOSSpecificIllegalChars()
    {
        if ( System.getProperty( "os.name" ).toLowerCase().startsWith( "win" ) )
        {
            return "\\/:*?\"<>|\0";
        }
        else
        {
            return "/\0";
        }
    }
}
