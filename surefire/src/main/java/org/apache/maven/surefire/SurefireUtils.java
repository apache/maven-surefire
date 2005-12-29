package org.apache.maven.surefire;

import org.apache.maven.surefire.battery.JUnitBattery;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * @author Jason van  Zyl
 * @version $Id$
 */
public class SurefireUtils
{
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
        else
        {
            battery = new JUnitBattery( testClass, loader );
        }

        return battery;
    }
}
