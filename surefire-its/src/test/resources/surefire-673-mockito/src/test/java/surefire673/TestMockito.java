package surefire673;

import org.junit.Test;
import org.mockito.Mockito;

public class TestMockito
{
    @Test
    public void canMockPrivateStaticClass()
    {
        Mockito.mock(PrivateClass.class);
    }

    private static class PrivateClass
    {
    }
}
