package org.apache.maven.surefire.api.util;

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
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Centralized file management of temporary files in surefire.<br>
 * Files are deleted on VM exit.
 *
 * @author Markus Spann
 */
public final class SureFireFileManager
{

    private static TempFileManager instance = create();

    private static TempFileManager create()
    {
        String subDirName = "surefire";

        // create directory name suffix from legal chars in the current user name
        // or a millisecond timestamp as fallback
        String userSuffix = Stream.of( "user.name", "USER", "USERNAME" )
                        .map( System::getProperty )
                        .filter( Objects::nonNull )
                        .findFirst()
                        .map( u -> u.replaceAll( "[^A-Za-z0-9\\-_]", "" ) )
                        .map( u -> u.isEmpty() ? null : u )
                        .orElse( Long.toString( System.currentTimeMillis() ) );

        if ( userSuffix != null )
        {
            subDirName += "-" + userSuffix;
        }

        TempFileManager tfm = TempFileManager.instance( subDirName );
        tfm.setDeleteOnExit( true );
        return tfm;
    }

    public static File createTempFile( String prefix, String suffix )
    {
        return instance.createTempFile( prefix, suffix );
    }

}
