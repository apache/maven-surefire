package org.apache.maven.plugins.surefire.dumppid;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal dumps the PID of the maven process
 */
@Mojo( name = "dump-pid", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES )
public class DumpPidMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${project.build.directory}", property = "dumpPid.targetDir" )
    private File targetDir;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            getLog().info( "Dumping PID to " + targetDir );
            
            if ( !targetDir.exists() )
            {
                targetDir.mkdirs();
            }
            
            File target = new File( targetDir, "maven.pid" ).getCanonicalFile();

            try ( FileWriter fw = new FileWriter( target ) )
            {
                String pid = ManagementFactory.getRuntimeMXBean().getName();
                fw.write( pid );
                getLog().info( "Wrote " + pid + " to " + target );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create pid file", e );
        }
    }
}
