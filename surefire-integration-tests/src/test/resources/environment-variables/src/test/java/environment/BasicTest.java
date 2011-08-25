package environment;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;


public class BasicTest
{

    
    @Test
    public void testEnvVar()
    {
        Assert.assertThat( System.getenv( "PATH" ), notNullValue() );
        Assert.assertThat( System.getenv( "DUMMY_ENV_VAR" ), is( "foo" ) );
    }


}
