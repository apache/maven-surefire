package org.apache.maven.surefire.booter.output;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Surefire output consumer that will take out the surefire header
 * 
 * @since 2.1
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class SupressHeaderOutputConsumerProxy
    extends OutputConsumerProxy
{

    /**
     * Create a consumer that will delegate all calls to the next filter but {@link #consumeHeaderLine(String)}
     * 
     * @param nextFilter filter to delegate to
     */
    public SupressHeaderOutputConsumerProxy( OutputConsumer nextFilter )
    {
        super( nextFilter );
    }

    /**
     * Do nothing
     */
    public void consumeHeaderLine( String line )
    {
        /* do nothing */
    }

}
