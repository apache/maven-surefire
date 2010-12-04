package org.apache.maven.surefire.booter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Stephen Conolly
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class ProviderDetector
{
    /**
     * Method loadServices loads the services of a class that are
     * defined using the SPI mechanism.
     *
     * @param clazz       The interface / abstract class defining the service.
     * @param classLoader of type ClassLoader the classloader to use.
     * @return An array of instances.
     * @throws IOException When unable to read/load the manifests
     */
    public static Object[] loadServices( Class clazz, ClassLoader classLoader )
        throws IOException
    {
        final String resourceName = "META-INF/services/" + clazz.getName();

        if ( classLoader == null )
        {
            return new Object[0];
        }
        final Enumeration urlEnumeration = classLoader.getResources( resourceName );
        final Set names = getNames( urlEnumeration );
        if ( names == null || names.size() == 0 )
        {
            return (Object[]) Array.newInstance( clazz, 0 );
        }

        return instantiateServices( clazz, classLoader, names );
    }

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

    private static Object[] instantiateServices( Class clazz, ClassLoader classLoader, Set names )
    {
        List result = new ArrayList();
        for ( Iterator i = names.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();
            try
            {
                Class implClass = classLoader.loadClass( name );
                if ( !clazz.isAssignableFrom( implClass ) )
                {
                    continue;
                }
                result.add( implClass.newInstance() );
            }
            catch ( ClassNotFoundException e )
            {
                // ignore
            }
            catch ( IllegalAccessException e )
            {
                // ignore
            }
            catch ( InstantiationException e )
            {
                // ignore
            }
        }
        return result.toArray( (Object[]) Array.newInstance( clazz, result.size() ) );
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
