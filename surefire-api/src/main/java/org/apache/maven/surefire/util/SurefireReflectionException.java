package org.apache.maven.surefire.util;

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
 * Exception indicating that surefire had problems with reflection. This may be due
 * to programming errors, incorrect configuration or incorrect dependencies, but is
 * generally not recoverable and not relevant to handle.
 *
 * @author Kristian Rosenvold
 */
public class SurefireReflectionException
    extends RuntimeException
{
    /**
     * Create a {@link SurefireReflectionException} with the specified cause. The method {@link #getMessage} of this
     * exception object will return {@code cause == null ? "" : cause.toString()}.
     *
     * @param cause The cause of this exception
     */
    public SurefireReflectionException( Throwable cause )
    {
        super( cause == null ? "" : cause.toString(), cause );
    }
}
