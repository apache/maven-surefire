package org.codehaus.surefire.battery;

import org.codehaus.surefire.util.DirectoryScanner;
import org.codehaus.surefire.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryBattery
    extends AbstractBattery
{
    private static final String FS = System.getProperty( "file.separator" );

    private String basedir;

    private List includes;

    private List excludes ;

    public DirectoryBattery( String basedir, ArrayList includes, ArrayList excludes )
        throws Exception
    {
        this.basedir = basedir;

        this.includes = includes;

        this.excludes = excludes;

        discoverBatteryClassNames();
    }

    public void discoverBatteryClassNames()
        throws Exception
    {
        String[] tests = collectTests( basedir, includes, excludes );

        for ( int i = 0; i < tests.length; i++ )
        {
            String s = tests[i];

            s = s.substring( 0, s.indexOf( "." ) );

            s = s.replace( FS.charAt( 0 ), ".".charAt( 0 ) );

            addSubBatteryClassName( s );
        }
    }

    public String[] collectTests( String basedir, List includes, List excludes )
        throws Exception
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( new File( basedir, "target/test-classes" ) );

        if ( includes != null )
        {
            String[] incs = new String[includes.size()];

            for ( int i = 0; i < incs.length; i++ )
            {
                incs[i] = StringUtils.replace( (String) includes.get( i ), "java", "class" );

            }

            scanner.setIncludes( incs );
        }

        if ( excludes != null )
        {
            String[] excls = new String[excludes.size() + 1];

            for ( int i = 0; i < excls.length - 1; i++ )
            {
                excls[i] = StringUtils.replace( (String) excludes.get( i ), "java", "class" );
            }

            // Exclude inner classes

            excls[excludes.size()] = "**/*$*";

            scanner.setExcludes( excls );
        }

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
