package test;

import main.Service;
import org.junit.Test;

import java.io.IOException;
import java.util.Scanner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class MyIT
{
    @Test
    public void test() throws Exception
    {
        Service service = new Service();
        String moduleName = service.getClass().getModule().getName();
        System.out.println( service.getClass() + " in the module \"" + moduleName + "\"" );
        assertThat( moduleName, is( "main" ) );

        moduleName = getClass().getModule().getName();
        System.out.println( getClass() + " in the module \"" + moduleName + "\"" );
        assertThat( moduleName, is( "test" ) );

        System.out.println( service.getNormalResource() );
        assertThat( service.getNormalResource(), is( "Hi there!" ) );

        System.out.println( service.getResourceByJPMS() );
        assertThat( service.getResourceByJPMS(), is( "Hi there!" ) );

        System.out.println( getNormalResource() );
        assertThat( getNormalResource(), is( "Hello!" ) );

        System.out.println( getResourceByJPMS() );
        assertThat( getResourceByJPMS(), is( "Hello!" ) );

        Module main = ModuleLayer.boot()
            .modules()
            .stream()
            .filter( m -> hasResource( m, "main/a.txt" ) )
            .findFirst()
            .get();
        assertThat( getResourceByModule( main, "main/a.txt" ), is( "Hi there!" ) );
        assertThat( getMainResource(), is( "Hi there!" ) );
    }

    private String getNormalResource()
    {
        try ( Scanner scanner = new Scanner( getClass().getResourceAsStream( "/tests/a.txt" ) ) )
        {
            return scanner.nextLine();
        }
    }

    private String getResourceByJPMS() throws IOException
    {
        try ( Scanner scanner = new Scanner( getClass().getModule().getResourceAsStream( "tests/a.txt" ) ) )
        {
            return scanner.nextLine();
        }
    }

    private String getResourceByModule( Module module, String resource ) throws IOException
    {
        try ( Scanner scanner = new Scanner( module.getResourceAsStream( resource ) ) )
        {
            return scanner.nextLine();
        }
    }

    private String getMainResource()
    {
        try ( Scanner scanner = new Scanner( Service.class.getResourceAsStream( "/main/a.txt" ) ) )
        {
            return scanner.nextLine();
        }
    }

    private static boolean hasResource( Module module, String resource )
    {
        try
        {
            return module.getResourceAsStream( resource ) != null;
        }
        catch ( IOException e )
        {
            return false;
        }
    }
}
