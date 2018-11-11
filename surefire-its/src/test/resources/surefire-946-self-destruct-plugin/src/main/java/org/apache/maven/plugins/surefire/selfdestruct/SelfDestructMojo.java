package org.apache.maven.plugins.surefire.selfdestruct;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

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
 * Goal which terminates the maven process it is executed in after a timeout.
 * 
 * @goal selfdestruct
 * @phase test
 */
public class SelfDestructMojo
    extends AbstractMojo
{
    private enum DestructMethod
    {
        exit, halt, interrupt;
    }

    /**
     * Timeout in milliseconds
     * 
     * @parameter
     */
    private long timeoutInMillis = 0;

    /**
     * Method of self-destruction: 'exit' will use System.exit (default), 'halt' will use Runtime.halt, 'interrupt' will
     * try to call 'taskkill' (windows) or 'kill -INT' (others)
     * 
     * @parameter
     */
    private String method = "exit";

    public void execute()
        throws MojoExecutionException
    {

        DestructMethod destructMethod = DestructMethod.valueOf( method );

        if ( timeoutInMillis > 0 )
        {
            getLog().warn( "Self-Destruct in " + timeoutInMillis + " ms using " + destructMethod );
            Timer timer = new Timer( "", true );
            timer.schedule( new SelfDestructionTask( destructMethod ), timeoutInMillis );
        }
        else
        {
            new SelfDestructionTask( destructMethod ).run();
        }
    }

    private void selfDestruct( DestructMethod destructMethod )
    {
        getLog().warn( "Self-Destructing NOW." );
        switch ( destructMethod )
        {
            case exit:
                System.exit( 1 );
            case halt:
                Runtime.getRuntime().halt( 1 );
            case interrupt:
                String name = ManagementFactory.getRuntimeMXBean().getName();
                int indexOfAt = name.indexOf( '@' );
                if ( indexOfAt > 0 )
                {
                    String pid = name.substring( 0, indexOfAt );
                    getLog().warn( "Going to kill process with PID " + pid );

                    List<String> args = new ArrayList<>();
                    if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
                    {
                        args.add( "taskkill" );
                        args.add( "/PID" );
                    }
                    else
                    {
                        args.add( "kill" );
                        args.add( "-INT" );
                    }
                    args.add( pid );

                    try
                    {
                        new ProcessBuilder( args ).start();
                    }
                    catch ( IOException e )
                    {
                        getLog().error( "Unable to spawn process. Killing with System.exit.", e );
                    }
                }
                else
                {
                    getLog().warn( "Unable to determine my PID... Using System.exit" );
                }
        }

        System.exit( 1 );
    }

    private class SelfDestructionTask
        extends TimerTask
    {

        private DestructMethod destructMethod;

        public SelfDestructionTask( DestructMethod destructMethod )
        {
            this.destructMethod = destructMethod;
        }

        @Override
        public void run()
        {
            selfDestruct( destructMethod );
        }

    }
}
