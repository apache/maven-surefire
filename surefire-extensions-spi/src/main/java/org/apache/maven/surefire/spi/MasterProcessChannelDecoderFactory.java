package org.apache.maven.surefire.spi;

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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.providerapi.MasterProcessChannelDecoder;

import java.io.IOException;

/**
 * The SPI interface, a factory of decoders.
 */
public interface MasterProcessChannelDecoderFactory
{
    /**
     * Decoder factory method.
     *
     * @param channelConfig "pipe:std:in" or "tcp://localhost:65035"
     * @param logger        error logger
     * @return a new instance of decoder
     */
    MasterProcessChannelDecoder createDecoder( String channelConfig, ConsoleLogger logger ) throws IOException;
}
