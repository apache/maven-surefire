package org.apache.maven.plugin.surefire.util;

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

import org.apache.commons.lang3.StringUtils;
import javax.annotation.Nonnull;

final class ScannerUtil
{

    private ScannerUtil()
    {
        throw new IllegalStateException( "not instantiable constructor" );
    }

    @Deprecated
    private static final String FS = System.getProperty( "file.separator" );

    @Deprecated
    private static final boolean IS_NON_UNIX_FS = ( !FS.equals( "/" ) );

    @Nonnull public static String convertJarFileResourceToJavaClassName( @Nonnull String test )
    {
        return StringUtils.removeEnd( test, ".class" ).replace( "/", "." );
    }

    public static boolean isJavaClassFile( String file )
    {
        return file.endsWith( ".class" );
    }

    @Deprecated
    @Nonnull public static String convertSlashToSystemFileSeparator( @Nonnull String path )
    {
        return ( IS_NON_UNIX_FS ? path.replace( "/", FS ) : path );
    }
}
