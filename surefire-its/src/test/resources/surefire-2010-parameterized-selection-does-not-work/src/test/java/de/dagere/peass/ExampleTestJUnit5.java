package de.dagere.peass;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ExampleTestJUnit5
{

    @ParameterizedTest
    @ValueSource( ints = { 0, 1 } )
    public void test( final int value )
    {
        System.out.println( value );
    }

    @Test
    public void anotherTest()
    {
        System.out.println( "3" );
    }

}