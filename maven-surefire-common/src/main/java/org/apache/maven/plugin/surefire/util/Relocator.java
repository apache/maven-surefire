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


import javax.annotation.Nonnull;

/**
 * Relocates class names when running with relocated provider
 *
 * @author Kristian Rosenvold
 */
public final class Relocator
{
    private static final String RELOCATION_BASE = "org.apache.maven.surefire.";
    private static final String PACKAGE_DELIMITER = "shadefire";

    private Relocator()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    @Nonnull
    public static String relocate( @Nonnull String className )
    {
        if ( className.contains( PACKAGE_DELIMITER ) )
        {
            return className;
        }
        else
        {
            if ( !className.startsWith( RELOCATION_BASE ) )
            {
                throw new IllegalArgumentException( "'" + className + "' should start with '" + RELOCATION_BASE + "'" );
            }
            String rest = className.substring( RELOCATION_BASE.length() );
            final String s = RELOCATION_BASE + PACKAGE_DELIMITER + ".";
            return s + rest;
        }
    }
}
