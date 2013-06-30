package org.apache.maven.surefire.junitcore;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import junit.runner.Version;
import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.13
 */
final class JUnitUtils
{
    private JUnitUtils()
    {
    }

    private static List<Integer> parseVersion( String version )
    {
        ArrayList<Integer> versions = new ArrayList<Integer>( 3 );
        StringTokenizer tokens = new StringTokenizer( version, "." );
        try
        {
            while ( tokens.hasMoreTokens() )
            {
                versions.add( Integer.valueOf( tokens.nextToken() ) );
            }
        }
        catch ( NumberFormatException e )
        {
            throw new NestedRuntimeException( "JUnit version has not numbers " + version, e );
        }
        return versions;
    }

    /**
     * @return Maven versions ordered as follows (if exist):
     * <ul>
     *   <li> Major at index 0
     *   <li> Minor at index 1
     *   <li> Incremental at index 2
     * </ul>
     * @throws NestedRuntimeException if cannot parse the version; or the version is null
     */
    static List<Integer> versions()
    {
        String version = Version.id();System.out.println("tibor " + version);

        if ( version == null )
        {
            throw new NestedRuntimeException( new NullPointerException( "junit.runner.Version#id returns null" ) );
        }

        if ( version.contains( "-" ) )
        {
            version = version.substring( 0, version.indexOf( '-' ) );
        }

        return parseVersion( version );
    }

    static boolean isCompatibleVersionWith( int expectedMajor, int minor )
    {
        Iterator<Integer> currentVersions = versions().iterator();

        if ( !currentVersions.hasNext() || currentVersions.next() != expectedMajor )
        {
            return false;
        }

        if ( !currentVersions.hasNext() || currentVersions.next() < minor )
        {
            return false;
        }

        return true;
    }
}
