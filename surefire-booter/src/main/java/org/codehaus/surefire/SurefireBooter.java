package org.codehaus.surefire;

/*
 * Copyright 2001-2005 The Codehaus.
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SurefireBooter
{
    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();

    private String reportsDirectory;

    private String forkMode;

    public SurefireBooter()
    {
    }

    public void setReportsDirectory( String reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public void addBattery( String battery, Object[] params )
    {
        batteries.add( new Object[]{ battery, params } );
    }

    public void addBattery( String battery )
    {
        batteries.add( new Object[]{ battery, null } );
    }

    public void addReport( String report )
    {
        reports.add( report );
    }

    public void addClassPathUrl( String path )
    {
        if ( !classpathUrls.contains( path ) )
        {
            classpathUrls.add( path );
        }
    }

    public void setClassPathUrls( List classpathUrls )
    {
        this.classpathUrls = classpathUrls;
    }

    public void setForkMode(String forkMode)
    {
        this.forkMode = forkMode;
    }

    public boolean run()
        throws Exception
    {
      boolean result = false;

      if ("once".equals(forkMode))
      {
          result = runTestsForkOnce();
      }
      else if ("none".equals(forkMode))
      {
          result = runTestsInProcess();
      }
      else if ("per_test".equals(forkMode))
      {
          result = runTestsForkEach();
      }
      else
      {
        // throw
      }

      return result;
    }

    private boolean runTestsInProcess() throws Exception
    {
        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader();

        for ( Iterator i = classpathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url == null )
            {
                continue;
            }

            File f = new File( url );

            surefireClassLoader.addURL( f.toURL() );
        }

        Class batteryExecutorClass = surefireClassLoader.loadClass( "org.codehaus.surefire.Surefire" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[] { List.class, List.class, ClassLoader.class, String.class } );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        Boolean result = (Boolean) run.invoke( batteryExecutor, new Object[]{ reports, batteries, surefireClassLoader, reportsDirectory } );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

    private boolean runTestsForkOnce()
    {
        return true;
    }

    private boolean runTestsForkEach()
    {
        return true;
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }
}
