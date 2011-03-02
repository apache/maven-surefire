package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterConfiguration;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles timeout of the forked process. Basically disables reporting
 * when the timeout has expired, to avoid leaving incorrect files on disk.
 *
 * @author Kristian Rosenvold
 */
class ForkTimeout
{

    private final Timer timer;


    ForkTimeout( int timeOutInMs, ReporterConfiguration reporterConfiguration, SurefireProvider surefireProvider )
    {
        SurefireTimeoutMonitor timeoutTask = new SurefireTimeoutMonitor( reporterConfiguration, surefireProvider );
        timer = new Timer( "Surefire fork timeout timer" );
        timer.schedule( timeoutTask, timeOutInMs );
    }

    public void close()
    {
        timer.cancel();
    }

    private static class SurefireTimeoutMonitor
        extends TimerTask
    {
        private final ReporterConfiguration reporterConfiguration;
        private final SurefireProvider surefireProvider;


        public SurefireTimeoutMonitor( ReporterConfiguration reporterConfiguration, SurefireProvider surefireProvider )
        {
            this.reporterConfiguration = reporterConfiguration;
            this.surefireProvider = surefireProvider;
        }

        public void run()
        {
            reporterConfiguration.setTimedOut( true );
            surefireProvider.cancel();
        }
    }

}
