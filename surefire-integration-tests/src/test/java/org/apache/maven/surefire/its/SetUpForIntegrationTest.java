package org.apache.maven.surefire.its;
/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Stephen Connolly
 * @since 05-Jan-2010 08:17:17
 */
public class SetUpForIntegrationTest
    extends TestCase
{
    public void testSmokes()
        throws IOException
    {
        // if the properties are missing we'll fail the test with an NPE and stop the build.
        File originalSettings = new File( System.getProperty( "maven.settings.file" ) );
        File newRepo = new File( System.getProperty( "maven.staged.local.repo" ) );
        File newSettings = new File( originalSettings.getParentFile(), originalSettings.getName() + ".staged" );
        StagedLocalRepoHelper.createStagedSettingsXml( originalSettings, newRepo, newSettings );
    }
}
