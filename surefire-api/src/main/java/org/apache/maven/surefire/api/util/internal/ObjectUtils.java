package org.apache.maven.surefire.api.util.internal;

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

import java.lang.management.ManagementFactory;
import java.util.Map;

import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_16;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_RECENT;

/**
 * Similar to Java 7 java.util.Objects, and another utility methods.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class ObjectUtils
{
    private ObjectUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static <T> T useNonNull( T target, T fallback )
    {
        return target == null ? fallback : target;
    }

    public static Map<String, String> systemProps()
    {
        return ManagementFactory.getRuntimeMXBean().getSystemProperties();
    }

    /**
     * The {@link SecurityManager} is deprecated since Java 17.
     *
     * @return <code>true</code> if Java Specification Version is less than
     *         or equal to 16; <code>false</code> otherwise.
     */
    public static boolean isSecurityManagerSupported()
    {
        return JAVA_RECENT.atMost( JAVA_16 );
    }
}
