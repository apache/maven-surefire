package org.codehaus.surefire;

import java.net.URLClassLoader;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class IsolatedClassLoader
    extends URLClassLoader
{
    private ClassLoader parent = ClassLoader.getSystemClassLoader();

    private Set urls = new HashSet();

    public IsolatedClassLoader()
    {
        super( new URL[0], null );
    }

    public void addURL( URL url )
    {
        if ( !urls.contains( url ) )
        {
            super.addURL( url );
        }
        else
        {
            urls.add( url );
        }
    }

    public synchronized Class loadClass( String className )
        throws ClassNotFoundException
    {
        Class c = findLoadedClass( className );

        ClassNotFoundException ex = null;

        if ( c == null )
        {
            try
            {
                c = findClass( className );
            }
            catch ( ClassNotFoundException e )
            {
                ex = e;

                if ( parent != null )
                {
                    c = parent.loadClass( className );
                }
            }
        }

        if ( c == null )
        {
            throw ex;
        }

        return c;
    }
}
