package junitplatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;


public class ParametersTest
{
    public static Stream<ConnectionPoolFactory> pools()
    {
        return Stream.of( new ConnectionPoolFactory( "duplex" ),
            new ConnectionPoolFactory( "multiplex" ),
            new ConnectionPoolFactory( "round-robin" ) );
    }

    @ParameterizedTest
    @MethodSource( "pools" )
    public void testAllPassingTest( ConnectionPoolFactory factory )
    {
        System.out.println( "testAllPassingTest factory " + factory );
    }

    @ParameterizedTest
    @MethodSource( "pools" )
    public void testOneFailingPassingTest( ConnectionPoolFactory factory ) throws Exception
    {
        Assumptions.assumeFalse( factory.name.equals( "round-robin" ) );
        System.out.println( "Passing test factory " + factory );
        if ( factory.name.equals( "multiplex" ) )
        {
            assertEquals( 1, 2 );
        }
    }

    private static class ConnectionPoolFactory
    {
        private final String name;

        private ConnectionPoolFactory( String name )
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
