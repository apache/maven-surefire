package org.apache.maven.plugin.surefire;

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

import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.booter.SystemUtils.toJdkHomeFromJvmExec;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class JdkAttributes
{
    private final File jvmExecutable;
    private final File jdkHome;
    private final boolean java9AtLeast;

    public JdkAttributes( File jvmExecutable, File jdkHome, boolean java9AtLeast )
    {
        this.jvmExecutable = requireNonNull( jvmExecutable, "null path to java executable" );
        this.jdkHome = jdkHome;
        this.java9AtLeast = java9AtLeast;
    }

    public JdkAttributes( String jvmExecutable, boolean java9AtLeast )
    {
        this( new File( jvmExecutable ), toJdkHomeFromJvmExec( jvmExecutable ), java9AtLeast );
    }

    public File getJvmExecutable()
    {
        return jvmExecutable;
    }

    public File getJdkHome()
    {
        return jdkHome;
    }

    public boolean isJava9AtLeast()
    {
        return java9AtLeast;
    }
}
