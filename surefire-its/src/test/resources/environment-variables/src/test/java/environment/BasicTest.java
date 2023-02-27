package environment;

import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;


public class BasicTest
{

    
    @Test
    public void testEnvVar()
    {
        Assert.assertThat( System.getenv( "PATH" ), notNullValue() );
        Assert.assertThat( System.getenv( "DUMMY_ENV_VAR" ), is( "foo" ) );
        Assert.assertThat( System.getenv( "EMPTY_VAR" ), is( "" ) );
        Assert.assertThat( System.getenv( "UNSET_VAR" ), is( "" ) );
        Assert.assertThat( System.getenv( "UNDEFINED_VAR" ), nullValue() );
    }


}
