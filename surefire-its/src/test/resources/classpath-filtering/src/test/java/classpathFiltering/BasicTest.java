package classpathFiltering;

import junit.framework.TestCase;

public class BasicTest
    extends TestCase
{

    public void testDependencyFilter() {
        
        Class testClass = null;
        String testClassName = "org.apache.commons.mail.Email";
        try 
        {
            testClass = Class.forName( testClassName );
            System.out.println( "Able to load class " + testClass );
        }
        catch ( ClassNotFoundException e )
        {
            System.out.println( "Couldn't load " + testClassName );
        }
        assertNull( testClass );
    }

}
