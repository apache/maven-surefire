package jiras.surefire1036;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class TestSomethingWithMockitoRunner
{
    @Mock
    private List<Integer> mTestList;

    @Before
    public void setUp()
        throws Exception
    {
        when( mTestList.size() ).thenReturn( 5 );
    }

    @Test
    public void thisTestUsesMockitoRunnerButIsPrettyUseless()
        throws Exception
    {
        assertEquals( 5, mTestList.size() );
    }
}
