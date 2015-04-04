package jiras.surefire1024;

import org.junit.Test;

public class A1IT
{
    @Test
    public void test()
    {
        System.out.println( getClass() + "#test() dependency to scan" );
    }
}
