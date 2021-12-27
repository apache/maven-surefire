package org.apache.maven.surefire.shared.lang3;

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
 * Delegate for {@link org.apache.commons.lang3.SystemUtils}
 */
public class SystemUtils
{
    public static final boolean IS_OS_WINDOWS = org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
    public static final boolean IS_OS_HP_UX = org.apache.commons.lang3.SystemUtils.IS_OS_HP_UX;
    public static final boolean IS_OS_LINUX = org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
    public static final boolean IS_OS_UNIX = org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
    public static final boolean IS_OS_FREE_BSD = org.apache.commons.lang3.SystemUtils.IS_OS_FREE_BSD;
    public static final boolean IS_OS_NET_BSD = org.apache.commons.lang3.SystemUtils.IS_OS_NET_BSD;
    public static final boolean IS_OS_OPEN_BSD = org.apache.commons.lang3.SystemUtils.IS_OS_OPEN_BSD;

    public static final String JAVA_SPECIFICATION_VERSION =
        org.apache.commons.lang3.SystemUtils.JAVA_SPECIFICATION_VERSION;


}
