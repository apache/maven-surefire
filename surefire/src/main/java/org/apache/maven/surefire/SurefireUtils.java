package org.apache.maven.surefire;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.surefire.battery.JUnitBattery;
import org.apache.maven.surefire.battery.TestNGBattery;
import org.testng.internal.TestNGClassFinder;
import org.testng.internal.annotations.IAnnotationFinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * @author Jason van  Zyl
 * @version $Id$
 */
public class SurefireUtils
{
    private static final IAnnotationFinder annotationFinder;

    static
    {
        if ( System.getProperty( "java.version" ).indexOf( "1.5" ) > -1 )
        {
            annotationFinder = new org.testng.internal.annotations.JDK15AnnotationFinder();
        }
        else
        {
            System.out.println( "Using JDK14AnnotationFinder" );
            annotationFinder = new org.testng.internal.annotations.JDK14AnnotationFinder();
        }
    }

    /**
     * For testng javadoc annotations, sets the test source directory source.
     *
     * @param testSourceDirectory
     */
    public static void setTestSourceDirectory( String testSourceDirectory )
    {
        annotationFinder.addSourceDirs( new String[]{testSourceDirectory} );
    }

    public static Object instantiateBattery( Object[] holder, ClassLoader loader )
        throws Exception
    {
        Class testClass;

        Class batteryClass;

        try
        {
            testClass = loader.loadClass( (String) holder[0] );

            batteryClass = loader.loadClass( "org.apache.maven.surefire.battery.Battery" );
        }
        catch ( Exception e )
        {
            return null;
        }

        if ( Modifier.isAbstract( testClass.getModifiers() ) )
        {
            return null;
        }

        Object battery = null;

        if ( batteryClass.isAssignableFrom( testClass ) )
        {
            if ( holder[1] != null )
            {
                Object[] params = (Object[]) holder[1];

                Class[] paramTypes = new Class[params.length];

                for ( int j = 0; j < params.length; j++ )
                {
                    paramTypes[j] = params[j].getClass();
                }

                Constructor constructor = testClass.getConstructor( paramTypes );

                battery = constructor.newInstance( params );
            }
            else
            {
                battery = testClass.newInstance();
            }
        }
        else if ( TestNGClassFinder.isTestNGClass( testClass, annotationFinder ) )
        {
            battery = new TestNGBattery( testClass, loader );
        }
        else
        {
            battery = new JUnitBattery( testClass, loader );
        }

        return battery;
    }
}
