package org.apache.maven.surefire.booter.stream;

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

import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.stream.AbstractStreamEncoder;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING_BYTES;
import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_FOR_EVENTS_BYTES;

/**
 *
 */
public class EventEncoder extends AbstractStreamEncoder<ForkedProcessEventType>
{
    public EventEncoder( WritableBufferedByteChannel out )
    {
        super( out );
    }

    @Nonnull
    @Override
    protected final byte[] getEncodedMagicNumber()
    {
        return MAGIC_NUMBER_FOR_EVENTS_BYTES;
    }

    @Nonnull
    @Override
    protected final byte[] enumToByteArray( @Nonnull ForkedProcessEventType e )
    {
        return e.getOpcodeBinary();
    }

    @Nonnull
    @Override
    protected final byte[] getEncodedCharsetName()
    {
        return DEFAULT_STREAM_ENCODING_BYTES;
    }

    @Nonnull
    @Override
    protected final Charset getCharset()
    {
        return DEFAULT_STREAM_ENCODING;
    }

    @Nonnull
    @Override
    protected final CharsetEncoder newCharsetEncoder()
    {
        return getCharset().newEncoder();
    }
}
