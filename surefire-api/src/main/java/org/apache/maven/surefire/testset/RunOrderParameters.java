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

import org.apache.maven.surefire.util.Randomizer;
import org.apache.maven.surefire.util.RunOrder;
import org.apache.maven.surefire.util.RunOrders;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderParameters
{
    private final RunOrders runOrders;

    private final File runStatisticsFile;

    private final Randomizer randomizer;


    public RunOrderParameters( @Nonnull RunOrders runOrders,
                               @Nullable Randomizer randomizer,
                               @Nullable File runStatisticsFile )
    {
        this.runOrders = runOrders;
        this.randomizer = ensureRandomizer( runOrders, randomizer );
        this.runStatisticsFile = runStatisticsFile;
    }

    public static RunOrderParameters alphabetical()
    {
        return new RunOrderParameters(
                new RunOrders( RunOrder.ALPHABETICAL ),
                null,
                null
        );
    }

    public RunOrders getRunOrders()
    {
        return runOrders;
    }

    public File getRunStatisticsFile()
    {
        return runStatisticsFile;
    }

    public Randomizer getRandomizer()
    {
        return randomizer;
    }

    public boolean isRandomized()
    {
        return isRandomized( this.runOrders );
    }

    @Nullable
    private static Randomizer ensureRandomizer( @Nonnull RunOrders runOrders,
                                                @Nullable Randomizer randomizer )
    {
        Randomizer result = randomizer;
        if ( isRandomized( runOrders ) && result == null )
        {
            result = new Randomizer();
        }
        return result;
    }

    private static boolean isRandomized( @Nonnull RunOrders runOrders )
    {
        return runOrders.contains( RunOrder.RANDOM );
    }
}
