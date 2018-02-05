package testng.objectfactory;

import java.lang.reflect.Constructor;
import org.testng.IObjectFactory;

public class TestNGCustomObjectFactory implements IObjectFactory
{
    public Object newInstance( Constructor constructor, Object... params )
    {
        String testClassName = constructor.getDeclaringClass().getName();
        FileHelper.writeFile( "objectFactory-output.txt", "Instantiated Test: " + testClassName + "\n" );
        try
        {
            return constructor.newInstance( params );
        }
        catch ( Exception exception )
        {
            throw new RuntimeException( exception );
        }
    }

}