package org.apache.maven.surefire.util.internal;

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

import java.util.Random;

/**
 * Creates a randomized simple long, using fast random
 *
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2016-04-01
 */
public class UniqIdGenerator
{
    private final Random random;
    private final int lowerBound;
    private final int upperBound;

    public UniqIdGenerator( int lowerBound, int upperBound )
    {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.random = getUnsecuredFastRandom();
    }

    public long generateUniqId()
    {
        int next = random.nextInt( upperBound - lowerBound );
        next += lowerBound;
        return next;
    }

    private static Random getUnsecuredFastRandom()
    {
        return new Random( System.currentTimeMillis() );
    }
}
