package org.apache.maven.surefire.api.report;

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

import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Segment;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Determines the purpose the provider started the tests. It can be either normal run or a kind of re-run type.
 * <br>
 * This is important in the logic of {@code StatelessXmlReporter}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public enum RunMode
{
    NORMAL_RUN( "normal-run" ),
    RERUN_TEST_AFTER_FAILURE( "rerun-test-after-failure" );
    //todo add here RERUN_TESTSET, see https://github.com/apache/maven-surefire/pull/221

    // due to have fast and thread-safe Map
    public static final Map<Segment, RunMode> RUN_MODES = segmentsToRunModes();

    private final String runmode;
    private final byte[] runmodeBinary;

    RunMode( String runmode )
    {
        this.runmode = runmode;
        runmodeBinary = runmode.getBytes( US_ASCII );
    }

    public String geRunmode()
    {
        return runmode;
    }

    public byte[] getRunmodeBinary()
    {
        return runmodeBinary;
    }

    private static Map<Segment, RunMode> segmentsToRunModes()
    {
        Map<Segment, RunMode> runModes = new HashMap<>();
        for ( RunMode runMode : RunMode.values() )
        {
            byte[] array = runMode.getRunmodeBinary();
            runModes.put( new Segment( array, 0, array.length ), runMode );
        }
        return runModes;
    }
}
