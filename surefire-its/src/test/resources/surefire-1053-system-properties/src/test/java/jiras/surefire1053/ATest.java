package jiras.surefire1053;

import org.junit.Test;

public final class ATest {

    @Test
    public void someMethod() throws InterruptedException {
        System.out.println( "file.encoding=" + System.getProperty( "file.encoding" ) );
        System.out.println( "myArg=" + System.getProperty( "myArg" ) );
    }

}
