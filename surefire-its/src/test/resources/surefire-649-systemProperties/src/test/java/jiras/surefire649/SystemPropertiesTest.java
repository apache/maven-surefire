package jiras.surefire649;

import org.junit.Test;

public final class SystemPropertiesTest {

    @Test
    public void someMethod() throws InterruptedException {
        String prop = System.getProperty( "emptyProperty" );
        System.out.println( "emptyProperty=" + ( prop == null ? null : "'" + prop + "'" ) );
    }

}
