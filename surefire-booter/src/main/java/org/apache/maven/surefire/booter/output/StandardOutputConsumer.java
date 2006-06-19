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

import java.io.PrintWriter;

/**
 * Surefire output consumer that writes everything to {@link System#out}
 * 
 * @since 2.1
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class StandardOutputConsumer
    extends PrintWriterOutputConsumer
{

    /**
     * Create a consumer that will write to {@link System#out}
     */
    public StandardOutputConsumer()
    {
        setPrintWriter( new PrintWriter( System.out ) );
    }

}
