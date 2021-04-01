package org.apache.maven.surefire.api.booter;

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

/**
 * @author Kristian Rosenvold
 */
public class ProviderParameterNames
{
    public static final String TESTNG_EXCLUDEDGROUPS_PROP = "excludegroups";

    public static final String TESTNG_GROUPS_PROP = "groups";

    public static final String INCLUDE_JUNIT5_ENGINES_PROP = "includejunit5engines";

    public static final String EXCLUDE_JUNIT5_ENGINES_PROP = "excludejunit5engines";

    public static final String THREADCOUNT_PROP = "threadcount";

    public static final String PARALLEL_PROP = "parallel";

    public static final String THREADCOUNTSUITES_PROP = "threadcountsuites";

    public static final String THREADCOUNTCLASSES_PROP = "threadcountclasses";

    public static final String THREADCOUNTMETHODS_PROP = "threadcountmethods";

    public static final String PARALLEL_TIMEOUT_PROP = "paralleltimeout";

    public static final String PARALLEL_TIMEOUTFORCED_PROP = "paralleltimeoutforced";

    public static final String PARALLEL_OPTIMIZE_PROP = "paralleloptimization";

}
