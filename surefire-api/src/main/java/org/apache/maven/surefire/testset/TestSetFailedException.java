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


/**
 * Exception that indicates a test failed.
 *
 * @author Bill Venners
 */
public class TestSetFailedException
    extends Exception
{

    /**
     * Creates {@code TestSetFailedException} with a detail message.
     *
     * @param message A detail message for this {@code TestSetFailedException}, or
     *                {@code null}. If {@code null} is passed, the {@link #getMessage}
     *                method will return an empty {@link String string}.
     */
    public TestSetFailedException( String message )
    {
        super( message );
    }

    /**
     * Creates {@code TestSetFailedException} with the specified detail
     * message and cause.
     * <br>
     * <p>Note that the detail message associated with cause is
     * <b>NOT</b> automatically incorporated in this throwable's detail
     * message.
     *
     * @param message A detail message for this {@code TestSetFailedException}, or {@code null}.
     * @param cause   the cause, which is saved for later retrieval by the {@link #getCause} method.
     *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public TestSetFailedException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates {@code TestSetFailedException} with the specified cause. The mthod {@link #getMessage} method of this
     * exception object will return {@code cause == null ? "" : cause.toString()}.
     *
     * @param cause The cause
     */
    public TestSetFailedException( Throwable cause )
    {
        super( cause == null ? "" : cause.toString(), cause );
    }
}
