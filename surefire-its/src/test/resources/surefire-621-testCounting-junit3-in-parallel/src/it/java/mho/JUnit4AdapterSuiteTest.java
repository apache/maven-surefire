package mho;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@Suite.SuiteClasses( {
    MySuiteTest1.class,
    MySuiteTest2.class,
    MySuiteTest3.class
} )
@RunWith( Suite.class )
public class JUnit4AdapterSuiteTest
{
    public static Test suite()
    {
        return new JUnit4TestAdapter( JUnit4AdapterSuiteTest.class );
    }
}
