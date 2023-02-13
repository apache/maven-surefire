package test;

import main.Service;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class MyTest
{
    @Test
    public void test()
    {
        Service service = new Service();
        String moduleName = service.getClass().getModule().getName();
        System.out.println( service.getClass() + " in the module \"" + moduleName + "\"" );
        assertThat( moduleName )
                .isEqualTo( "main" );

        moduleName = getClass().getModule().getName();
        System.out.println( getClass() + " in the module \"" + moduleName + "\"" );
        assertThat( moduleName )
                .isEqualTo( "test" );
    }
}
