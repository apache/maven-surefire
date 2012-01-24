package org.apache.maven.surefire;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.surefire.util.ScannerFilter;
import org.codehaus.plexus.util.SelectorUtils;

public class SpecificTestClassFilter
    implements ScannerFilter
{

    private static final char FS = System.getProperty( "file.separator" ).charAt( 0 );

    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private Set names;

    public SpecificTestClassFilter( String[] classNames )
    {
        if ( classNames != null && classNames.length > 0 )
        {
            this.names = new HashSet();
            for ( int i = 0; i < classNames.length; i++ )
            {
                String name = classNames[i];
                names.add( name );
            }
        }
    }

    public boolean accept( Class testClass )
    {
        // If the tests enumeration is empty, allow anything.
        boolean result = true;

        if ( names != null && !names.isEmpty() )
        {
            String className = testClass.getName().replace( '.', FS ) + JAVA_CLASS_FILE_EXTENSION;

            boolean found = false;
            for ( Iterator it = names.iterator(); it.hasNext(); )
            {
                String pattern = (String) it.next();

                // This is the same utility used under the covers in the plexus DirectoryScanner, and
                // therefore in the surefire DefaultDirectoryScanner implementation.
                if ( SelectorUtils.matchPath( pattern, className, true ) )
                {
                    found = true;
                    break;
                }
            }

            if ( !found )
            {
                result = false;
            }
        }

        return result;
    }

}
