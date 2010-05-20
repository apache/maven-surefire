package org.apache.maven.surefire.report;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Kristian Rosenvold
 */
public class RunStatistics
    extends TestSetStatistics
{


    /**
     * Holds the source(s) that causes the error(s).
     */
    private final Collection errorSources = new ArrayList();

    /**
     * Holds the source(s) that causes the failure(s).
     */
    private final Collection failureSources = new ArrayList();


    public void addErrorSource( String errorSource )
    {
        synchronized ( errorSources )
        {
            errorSources.add( errorSource );
        }
    }

    public void addFailureSource( String errorSource )
    {
        synchronized ( failureSources )
        {
            failureSources.add( errorSource );
        }
    }


    public Collection getFailureSources()
    {
        synchronized ( failureSources )
        {
            return Collections.unmodifiableCollection( failureSources );
        }
    }

    public Collection getErrorSources()
    {
        synchronized ( errorSources )
        {
            return Collections.unmodifiableCollection( errorSources );
        }
    }


}
