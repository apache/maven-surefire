package org.apache.maven.plugin.surefire.log.api;

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
 * Allows providers to write console messages on the running maven process.
 * <br>
 * This output is associated with the entire test run and not a specific
 * test, which means it just goes "straight" to the console "immediately".
 * <br>
 * This interface is used in org.apache.maven.plugin.surefire.CommonReflector and reflected
 * via IsolatedClassLoader which can see classes from JRE only. This interface MUST use
 * JRE types in method signatures, e.g. {@link String} or {@link Throwable}, etc.
 */
public interface ConsoleLogger
{
    boolean isDebugEnabled();

    void debug( String message );

    boolean isInfoEnabled();

    void info( String message );

    boolean isWarnEnabled();

    void warning( String message );

    boolean isErrorEnabled();

    /**
     * @param message          message to log
     */
    void error( String message );

    /**
     * Simply delegates to {@link #error(String) error( toString( t, message ) )}.
     *
     * @param message          message to log
     * @param t                exception, message and trace to log
     */
    void error( String message, Throwable t );

    /**
     * Simply delegates to method {@link #error(String, Throwable) error(null, Throwable)}.
     *
     * @param t                exception, message and trace to log
     */
    void error( Throwable t );
}
