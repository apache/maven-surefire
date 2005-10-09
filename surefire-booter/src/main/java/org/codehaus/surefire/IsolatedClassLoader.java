package org.codehaus.surefire;

import java.util.HashSet;
import java.net.URLClassLoader;

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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:andyglick@acm.org">Andy Glick</a>
 * @version $Id$
 */
public class IsolatedClassLoader
    extends URLClassLoader
{
    private ClassLoader parent = ClassLoader.getSystemClassLoader();

    private Set urls = new HashSet();

    /**
     * Constructs a new URLClassLoader for the specified URLs using the default
     * delegation parent <code>ClassLoader</code>. The URLs will be searched in
     * the order specified for classes and resources after first searching in
     * the parent class loader. Any URL that ends with a '/' is assumed to refer
     * to a directory. Otherwise, the URL is assumed to refer to a JAR file
     * which will be downloaded and opened as needed.
     * <p/>
     * <p>If there is a security manager, this method first calls the security
     * manager's <code>checkCreateClassLoader</code> method to ensure creation
     * of a class loader is allowed.
     *
     * @param urls the URLs from which to load classes and resources
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkCreateClassLoader</code> method
     *                           doesn't allow creation of a class loader.
     * @see SecurityManager#checkCreateClassLoader
     */
    public IsolatedClassLoader( URL[] urls, ClassLoader parent )
    {
        super( urls );
        this.parent = parent;
    }

    public IsolatedClassLoader( URL[] urls)
    {
        super(urls);
    }

    /**
     *
     */
    public IsolatedClassLoader()
    {
        super( new URL[0], null );
    }

    /**
     *
     * @param url
     */
    public void addURL( URL url )
    {
        if ( urls.contains( url ) )
        {
            urls.add( url );
        }
        else
        {
            super.addURL( url );
        }
    }

    /**
     *
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
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
