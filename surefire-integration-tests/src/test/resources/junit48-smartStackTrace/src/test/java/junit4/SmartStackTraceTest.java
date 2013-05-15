package junit4;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class SmartStackTraceTest
{

    @Test(expected = RuntimeException.class)
    public void shouldFailInMethodButDoesnt()
    {
    }

    @Test(expected = IOException.class)
    public void incorrectExceptionThrown()
    {
        throw new RuntimeException("We fail here");
    }

    @Test(expected = IOException.class)
    public void shortName()
    {
    }

}
