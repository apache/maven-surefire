package org.apache.maven.plugins.surefire.report;

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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Creates a nicely formatted Surefire Test Report in html format.
 * This goal does not run the tests, it only builds the reports.
 * This is a workaround for
 * <a href="http://jira.codehaus.org/browse/SUREFIRE-257">http://jira.codehaus.org/browse/SUREFIRE-257</a>
 *
 * @author <a href="mailto:baerrach@gmail.com">Barrie Treloar</a>
 * @since 2.3
 */
@Mojo(name = "report-only")
@Execute(phase = LifecyclePhase.NONE)
public class SurefireReportOnlyMojo
    extends SurefireReportMojo
{
}
