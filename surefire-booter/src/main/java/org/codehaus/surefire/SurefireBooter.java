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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.net.URL;

public class SurefireBooter
{
    private static final Log log = LogFactory.getLog(SurefireBooter.class);

    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();

    private String reportsDirectory;

    private String forkMode;

    private String basedir;

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
        log.info("entered runTestsInProcess");

        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader();

        for ( Iterator i = classpathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url == null )
            {
                continue;
            }

            log.info("classpath filename is " + url);

            File f = new File( url );

            surefireClassLoader.addURL( f.toURL() );
        }

        Class batteryExecutorClass = surefireClassLoader.loadClass( "org.codehaus.surefire.Surefire" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[] { List.class, List.class, ClassLoader.class, String.class } );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        log.info("batteryExecutor is " + batteryExecutor);
        log.info("reports are " + reports);
        log.info("batteries/size() is " + batteries.size());

        if (batteries.size() > 0)
        {
          for (int i = 0; i < batteries.size(); i++)
          {
            Object[] array = (Object[]) batteries.get(i);

            log.info ("battery name is " + array[0]);

            Object[] parmArray = (Object[]) array[1];

            for (int j = 0; j < parmArray.length; j++)
            {
              log.info("parmArray[" + j + "] value is " + parmArray[j]);
            }
          }
        }

        log.info("surefireClassLoader's class is " + surefireClassLoader.getClass().getName());
        log.info("reportsDirectory is " + reportsDirectory);

        Boolean result = (Boolean) run.invoke( batteryExecutor, new Object[]{ reports, batteries, surefireClassLoader, reportsDirectory } );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

  protected static final String EOL = System.getProperty( "line.separator" );
  protected static final String PS = System.getProperty( "path.separator" );

  private boolean runTestsForkOnce()
      throws Exception
  {
      log.info( "entered runTestsForkOnce" );


      String executable = "java";

      String pathSeparator = System.getProperty("path.separator");

      String classpathEntries =  getListOfStringsAsString(classpathUrls, pathSeparator);

      // List argList = new ArrayList();

      // argList.add("-classpath");

      String quotedClasspath = Commandline.quoteArgument(classpathEntries);

      log.info("quotedClasspath = " + quotedClasspath);

      // argList.add(quotedClasspath);

      // argList.add("org.codehaus.surefire.Surefire");

      log.info( "executable is " + executable );

      // String[] args = new String[10];

      // File workingDirectory = new File("fileName");
      File workingDirectory = new File( "." );

      Commandline cli = new Commandline();

      basedir = workingDirectory.getAbsolutePath();

      log.info( "basedir = " + basedir );

      cli.setWorkingDirectory( basedir );

      cli.setExecutable( executable );

      String[] args = getForkArgs();

      // log.info( "runTestsForkOnce there are " + argList.size() + " arguments");

      // String[] args = new String[argList.size()];

      // Iterator argIterator = argList.iterator();

      // for( int i = 0; i < args.length; i++ )
      // {
        // args[ i ] = (String) argIterator.next();
        // log.info( "arg is " + args[ i ] );
      // }

      cli.addArguments( args );

      String[] returnedArgs = cli.getShellCommandline();

      for( int i = 0; i < returnedArgs.length; i ++ )
      {
        log.info("returned arg is " + returnedArgs[i]);
      }

      Writer stringWriter = new StringWriter();

      StreamConsumer out = new WriterStreamConsumer( stringWriter );

      StreamConsumer err = new WriterStreamConsumer( stringWriter );

      int returnCode;

      List messages = new ArrayList();

      boolean hamburger = true;

      if ( hamburger )
      {


        try
        {
            log.info("call CommandLineUtils.executeCommandLine");

            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );

            // messages = parseModernStream( new BufferedReader( new StringReader( stringWriter.toString() ) ) );
        }
        catch ( CommandLineException e )
        {
            // throw new SurefireException( "Error while executing forked tests.", e );
          throw new Exception( "Error while executing forked tests.", e );
        }

        catch ( Exception e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e );
        }

        if ( returnCode != 0 && messages.isEmpty() )
        {
            // TODO: exception?
            // messages.add( new SurefireError( "Failure executing forked tests,  but could not parse the error:"
            //  +  EOL + stringWriter.toString(), true ) );

          // messages.add( new Error( "Failure executing forked tests,  but could not parse the error:"
          //  +  EOL + stringWriter.toString() ) );
        }


        Iterator messageIterator = messages.iterator();

        while ( messageIterator.hasNext() )
        {
          Error error = (Error) messageIterator.next();

          System.out.println("error message is " + error.getMessage() );

        }


        String string = stringWriter.toString();

        if ( string != null && string.length() > 0 )
        {
            StringReader sr = new StringReader( string );

            BufferedReader br = new BufferedReader( sr );

            while ( (string = br.readLine()) != null )
            {
                System.out.println(string);
            }
        }
        else
        {
            System.out.println("string from process is null or length = 0");
        }
    }
    else
    {
      useRuntimeExec(workingDirectory, args);
    }

    return true;
  }


    private boolean runTestsForkEach()
    {
        return true;
    }

    private String[] getForkArgs()
    {
      // List reports
      // List batteryHolders
      // ClassLoader classLoader
      // List classpathUrls
      // String reportsDirectory
      // String forkMode

      String pathSeparator = System.getProperty("path.separator");

      String classpathEntries =  getListOfStringsAsString(classpathUrls, pathSeparator);

      String reportClassNames = getListOfStringsAsString(reports, ",");

      String[] batteryConfig = getStringArrayFromBatteries();

      String classLoaderName = "org.codehaus.surefire.IsolatedClassLoader";

      log.info( "classpathEntries = " + classpathEntries );
      log.info( "reportClassNames = " + reportClassNames );
      log.info( "classLoaderName = " + classLoaderName );
      log.info( "reportsDirectory = " + reportsDirectory );
      log.info( "batteryExecutorName = " + "org.codehaus.surefire.Surefire" );
      log.info( "forkMode = " + forkMode );

      // a battery is defined as the name of the class - followed by directory
      // and includes and excludes

      // battery = "<batteryClassName>|<directory name>|<includes>|<excludes>
      for ( int i = 0; i < batteryConfig.length; i++ )
      {
          log.info( "batteryConfig = " + batteryConfig[i]);
      }

      String[] argArray =
      {
          "-Djava.class.path=" + classpathEntries,
         "org.codehaus.surefire.ForkedSurefireLoader",
         "classpathEntries=" + classpathEntries,
         "reportClassNames=" + reportClassNames,
         "reportsDirectory=" + reportsDirectory,
         "batteryExecutorName=" + "org.codehaus.surefire.Surefire",
         "forkMode=" + forkMode,
         "batteryConfig=" + batteryConfig[0]
      };

      return argArray;
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    private String getClassPathAsString()
    {
        StringBuffer classpathBuffer = new StringBuffer();

        Iterator classpathUrlIterator = classpathUrls.iterator();

        String delimiter = "";

        while ( classpathUrlIterator.hasNext() )
        {
            URL classpathURL = (URL) classpathUrlIterator.next();

            classpathBuffer.append(  delimiter );
            classpathBuffer.append( classpathURL.toString() );
            delimiter = ",";
        }

        return new String( classpathBuffer );
    }

    private String getListOfStringsAsString(List listOfStrings,
      String delimiterParm)
    {
        StringBuffer stringBuffer = new StringBuffer();

        Iterator listOfStringsIterator = listOfStrings.iterator();

        String delimiter = "";

        while ( listOfStringsIterator.hasNext() )
        {
            String report = (String) listOfStringsIterator.next();

            stringBuffer.append( delimiter );
            stringBuffer.append( report );

            delimiter = delimiterParm;
        }

        return new String ( stringBuffer );
    }

    private String[] getStringArrayFromBatteries()
    {
      String[] batteryConfig = new String[batteries.size()];

      Iterator batteryIterator = batteries.iterator();

      StringBuffer batteryBuffer = new StringBuffer();

      String delimiter = "";

      int batteryCounter = 0;

      while ( batteryIterator.hasNext() )
      {
        Object[] batteryArray = (Object[]) batteryIterator.next();

        batteryBuffer.append((String) batteryArray[0]);

        if (batteryArray[1] != null)
        {
          Object[] batteryParms = (Object[]) batteryArray[1];
          for ( int i = 0; i < 3; i++ )
          {
              batteryBuffer.append( "|" );
              batteryBuffer.append( batteryParms[i]);
          }
        }
        batteryConfig[batteryCounter++] = new String( batteryBuffer );
      }

      return batteryConfig;
    }

    private boolean useRuntimeExec(File workingDirectory, String[] args)
      throws Exception
    {
      List argList = new ArrayList();

      argList.add("CMD.EXE");
      argList.add("/X");
      argList.add("/C");
      argList.add("java");

      List anotherArgList = Arrays.asList( args );

      argList.addAll( anotherArgList );

      Runtime runtime = Runtime.getRuntime();

      String[] longArgs = new String[argList.size()];

      argList.toArray(longArgs);

      Process p = runtime.exec(longArgs, null, workingDirectory);

      InputStream is = p.getInputStream();

      Reader reader = new InputStreamReader(is);

      BufferedReader br = new BufferedReader(reader);

      InputStream error = p.getErrorStream();

      Reader errorReader =  new InputStreamReader(error);

      BufferedReader er = new BufferedReader(errorReader);


      p.waitFor();

      int exitValue =  p.exitValue();

      log.info( "process exit value is " + exitValue);

      String line = null;

      log.info("stdout");

      while ((line = br.readLine()) != null )
      {
        log.info( line );
      }

      log.info("stderr");

      while ((line = er.readLine()) != null )
      {
        log.info( line );
      }

      return exitValue == 0;
    }
}
