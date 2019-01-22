package it;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainTest
{

    // The intent is to run these two tests in different JVMs

    @Test
    public void test1()
    {
        Main main = new Main();
        main.setId( "test1" );
        assertEquals( "test1", main.getId() );
    }

    @Test
    public void test2()
    {
        Main main = new Main();
        main.setId( "test2" );
        assertEquals( "test2", main.getId() );
    }

}

