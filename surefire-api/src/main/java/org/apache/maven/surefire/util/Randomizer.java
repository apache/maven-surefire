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

import org.apache.maven.surefire.util.internal.UniqIdGenerator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.zip.CRC32;

/**
 * An object that holds an random seed and random order
 *
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2016-04-02
 */
public class Randomizer
{
    public static final String DEFAULT_SEED = "";
    private static final int DECIMAL_RADIX = 10;
    private static final int UPPER_BOUND = 1000000;
    private static final int LOWER_BOUND = 100000;
    private static final UniqIdGenerator UNIQ_ID_GENERATOR = new UniqIdGenerator(
            LOWER_BOUND, UPPER_BOUND
    );
    private final String givenSeed;
    private final long calculatedSeed;

    /**
     * Default constructor
     */
    public Randomizer()
    {
        this( Randomizer.DEFAULT_SEED );
    }

    /**
     * Default constructor with a seed as a parameter.
     *
     * @param givenSeed a user given seed
     */
    public Randomizer( @Nullable String givenSeed )
    {
        this.givenSeed = ensureSeed( givenSeed );
        this.calculatedSeed = makeItInRange( calculateSeed( this.givenSeed ) );
    }

    /**
     * Copy constructor
     *
     * @param givenSeed      a user given seed
     * @param calculatedSeed a calculated seed
     */
    public Randomizer( @Nonnull String givenSeed, long calculatedSeed )
    {
        this.givenSeed = givenSeed;
        this.calculatedSeed = calculatedSeed;
    }

    public long getSeed()
    {
        return calculatedSeed;
    }

    @Nonnull
    public String getGivenSeed()
    {
        return givenSeed;
    }

    @Nonnull
    public Random getRandom()
    {
        return new Random( getSeed() );
    }

    @Nonnull
    private static String ensureSeed( @Nullable String givenSeed )
    {
        return givenSeed != null ? givenSeed : DEFAULT_SEED;
    }

    private static long calculateSeed( @Nonnull String givenSeed )
    {
        if ( DEFAULT_SEED.equals( givenSeed ) )
        {
            return UNIQ_ID_GENERATOR.generateUniqId();
        }
        try
        {
            return Long.valueOf( givenSeed, DECIMAL_RADIX );
        }
        catch ( NumberFormatException nfex )
        {
            CRC32 crc32 = new CRC32();
            crc32.update( givenSeed.getBytes() );
            return crc32.getValue();
        }
    }

    private static long makeItInRange( long seed )
    {
        long strippedDown = seed % UPPER_BOUND;
        if ( strippedDown < LOWER_BOUND )
        {
            strippedDown += LOWER_BOUND;
        }
        return strippedDown;
    }
}
