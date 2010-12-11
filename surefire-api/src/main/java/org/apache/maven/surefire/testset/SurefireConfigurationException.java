package org.apache.maven.surefire.testset;

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

import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * Exception indicating that surefire cannot be set up properly. This may be due
 * to incorrect configuration or incorrect dependencies.
 *
 * @author Kristian Rosenvold
 */
public class SurefireConfigurationException
    extends NestedRuntimeException
{
    /**
     * Create a <code>SurefireConfigurationException</code> with the specified detail
     * message and cause.
     * <p/>
     * <p>Note that the detail message associated with cause is
     * <em>not</em> automatically incorporated in this throwable's detail
     * message.
     *
     * @param message A detail message for this <code>SurefireConfigurationException</code>, or <code>null</code>.
     * @param cause   the cause, which is saved for later retrieval by the <code>getCause</code> method.
     *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public SurefireConfigurationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Create a <code>SurefireConfigurationException</code> with the specified cause.  The
     * <code>getMessage</code> method of this exception object will return
     * <code>(cause == null ? "" : cause.toString())</code>.
     */
    public SurefireConfigurationException( Throwable cause )
    {
        super( cause == null ? "" : cause.toString(), cause );
    }
}
