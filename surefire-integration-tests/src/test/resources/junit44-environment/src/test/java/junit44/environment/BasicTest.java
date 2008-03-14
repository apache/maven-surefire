package junit44.environment;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import org.junit.Assert;
import org.junit.Test;


public class BasicTest
{

    
    @Test
    public void testEnvVar()
    {
        Assert.assertThat( System.getenv( "PATH" ), notNullValue() );
        Assert.assertThat( System.getenv( "DUMMY_ENV_VAR" ), is( "foo" ) );
    }


}
