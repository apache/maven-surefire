package org.apache.maven.surefire.junitplatform;

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

import org.apache.maven.surefire.api.util.ReflectionUtils;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Launcher proxy which delays the most possible the initialization of the real JUnit
 * Launcher in order to avoid stream/stdout corruption due to early logging.
 */
class LazyLauncher implements Launcher, AutoCloseable
{
    private AutoCloseable launcherSession;

    private Launcher launcher;

    @Override
    public void registerTestExecutionListeners( TestExecutionListener... testExecutionListeners )
    {
        launcher().registerTestExecutionListeners( testExecutionListeners );
    }

    @Override
    public TestPlan discover( LauncherDiscoveryRequest launcherDiscoveryRequest )
    {
        return launcher().discover( launcherDiscoveryRequest );
    }

    @Override
    public void execute( LauncherDiscoveryRequest launcherDiscoveryRequest,
                         TestExecutionListener... testExecutionListeners )
    {
        launcher().execute( launcherDiscoveryRequest, testExecutionListeners );
    }

    private Launcher launcher()
    {
        if ( launcher == null )
        {
            try
            {
                Class<?> sessionClass = Class.forName( "org.junit.platform.launcher.LauncherSession" );
                launcherSession = ReflectionUtils.invokeGetter( LauncherFactory.class, null, "openSession" ); 
                launcher = ReflectionUtils.invokeGetter( sessionClass, launcherSession, "getLauncher" );
            }
            catch ( ClassNotFoundException e )
            {
                launcher = LauncherFactory.create();
            }
        }
        return launcher;
    }

    @Override
    public void close() throws Exception
    {
        if ( launcherSession != null )
        {
            launcherSession.close();
            launcherSession = null;
        }
        launcher = null;
    }
}
