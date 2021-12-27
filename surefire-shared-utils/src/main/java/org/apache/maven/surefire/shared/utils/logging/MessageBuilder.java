package org.apache.maven.surefire.shared.utils.logging;

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
 * Delegate for {@link org.apache.maven.shared.utils.logging.MessageBuilder}
 */
public class MessageBuilder
{

    private final org.apache.maven.shared.utils.logging.MessageBuilder buffer;

    public MessageBuilder( org.apache.maven.shared.utils.logging.MessageBuilder buffer )
    {

        this.buffer = buffer;
    }

    public MessageBuilder a( CharSequence value )
    {
        buffer.a( value );
        return this;
    }

    public MessageBuilder a( Object value )
    {
        buffer.a( value );
        return this;
    }

    public MessageBuilder strong( Object message )
    {
        buffer.strong( message );
        return this;
    }

    public MessageBuilder success( Object message )
    {
        buffer.success( message );
        return this;
    }

    public MessageBuilder failure( Object message )
    {
        buffer.failure( message );
        return this;
    }

    public MessageBuilder warning( Object message )
    {
        buffer.warning( message );
        return this;
    }

    public String toString()
    {
        return buffer.toString();
    }
}
