package org.apache.maven.plugin.surefire.booterclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stephen Conolly
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class ProviderDetector
{

    public static Set getServiceNames( Class clazz, ClassLoader classLoader )
        throws IOException
    {
        final String resourceName = "META-INF/services/" + clazz.getName();

        if ( classLoader == null )
        {
            return new HashSet(  );
        }
        final Enumeration urlEnumeration = classLoader.getResources( resourceName );
        return getNames( urlEnumeration );
    }


    /**
     * Method loadServices loads the services of a class that are
     * defined using the SPI mechanism.
     *
     * @param urlEnumeration The urls from the resource
     * @throws IOException When reading the streams fails
     * @return The set of service provider names
     */
    private static Set getNames( final Enumeration urlEnumeration )
        throws IOException
    {
        final Set names = new HashSet();
        nextUrl:
        while ( urlEnumeration.hasMoreElements() )
        {
            final URL url = (URL) urlEnumeration.nextElement();
            final BufferedReader reader = getReader( url );
            try
            {
                String line;
                while ( ( line = reader.readLine() ) != null )
                {
                    int ci = line.indexOf( '#' );
                    if ( ci >= 0 )
                    {
                        line = line.substring( 0, ci );
                    }
                    line = line.trim();
                    int n = line.length();
                    if ( n == 0 )
                    {
                        continue; // next line
                    }

                    if ( ( line.indexOf( ' ' ) >= 0 ) || ( line.indexOf( '\t' ) >= 0 ) )
                    {
                        continue nextUrl; // next url
                    }
                    char cp = line.charAt( 0 ); // should use codePointAt but this is JDK1.3
                    if ( !Character.isJavaIdentifierStart( cp ) )
                    {
                        continue nextUrl; // next url
                    }
                    for ( int i = 1; i < n; i++ )
                    {
                        cp = line.charAt( i );  // should use codePointAt but this is JDK1.3
                        if ( !Character.isJavaIdentifierPart( cp ) && ( cp != '.' ) )
                        {
                            continue nextUrl; // next url
                        }
                    }
                    if ( !names.contains( line ) )
                    {
                        names.add( line );
                    }
                }
            }
            finally
            {
                reader.close();
            }
        }

        return names;
    }


    private static BufferedReader getReader( URL url )
        throws IOException
    {
        final InputStream inputStream = url.openStream();
        final InputStreamReader inputStreamReader = new InputStreamReader( inputStream );
        return new BufferedReader( inputStreamReader );
    }


}
