package org.apache.maven.plugin.surefire.runorder;

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

import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.util.Randomizer;
import org.apache.maven.surefire.util.RunOrder;
import org.apache.maven.surefire.util.RunOrderArguments;
import org.apache.maven.surefire.util.RunOrderMapper;
import org.apache.maven.surefire.util.RunOrders;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2018-05-24
 */
@ParametersAreNonnullByDefault
public final class RunOrderProcessor
{
    private final RunOrderMapper runOrderMapper = new RunOrderMapper();

    public RunOrderParameters createRunOrderParameters( RunOrders runOrders,
                                                        File statisticsFile )
    {
        @Nullable String seed = extractSeedFromRunOrders( runOrders );
        Randomizer randomizer = new Randomizer( seed );
        return new RunOrderParameters(
                runOrders,
                randomizer,
                statisticsFile
        );
    }

    public RunOrders readRunOrders( String runOrder )
    {
        return runOrderMapper.fromString( runOrder );
    }

    @Nullable
    private String extractSeedFromRunOrders( RunOrders runOrders )
    {

        if ( !runOrders.contains( RunOrder.RANDOM ) )
        {
            return null;
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            RunOrderArguments args = runOrders.getArguments( RunOrder.RANDOM );
            for ( String arg : args.getPositional() )
            {
                sb.append( arg );
            }
            return sb.toString();
        }
    }

}
