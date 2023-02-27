package junitplatformenginejupiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "<< ✨ >>" )
class DisplayNameTest
{
    @Test
    @DisplayName( "73$71 ✔" )
    void test1()
            throws Exception
    {
        System.out.println( getClass().getDeclaredMethod( "test1" ).getAnnotation( DisplayName.class ).value() );
        System.out.println( getClass().getAnnotation( DisplayName.class ).value() );
    }

    @Test
    @DisplayName( "73$72 ✔" )
    void test2()
            throws Exception
    {
        System.out.println( getClass().getDeclaredMethod( "test2" ).getAnnotation( DisplayName.class ).value() );
        System.out.println( getClass().getAnnotation( DisplayName.class ).value() );
    }
}
