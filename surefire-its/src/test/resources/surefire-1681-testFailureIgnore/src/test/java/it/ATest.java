package it;

import static org.junit.Assert.fail;

import org.junit.Test;

public class ATest
{
    @Test
    public void possiblyFailing()
    {
        if ( Boolean.getBoolean( "shouldFail" ) )
        {
            fail( "Failing test" );
        }
    }

    @Test
    public void possiblyCrashing()
    {
        if ( Boolean.getBoolean( "shouldCrash" ) )
        {
            System.exit(1);
        }
    }
}
