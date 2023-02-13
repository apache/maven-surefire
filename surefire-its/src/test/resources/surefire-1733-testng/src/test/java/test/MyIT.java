package test;

import main.Service;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class MyIT
{
    @Test
    public void test()
    {
        Service service = new Service();
        String moduleName = service.getClass().getModule().getName();
        System.out.println( service.getClass() + " in the module \"" + moduleName + "\"" );
        assertThat( moduleName, is( "main" ) );

        moduleName = getClass().getModule().getName();
        System.out.println( getClass() + " in the module \"" + moduleName + "\"" );
        assertThat( moduleName, is( "test" ) );
    }
}
