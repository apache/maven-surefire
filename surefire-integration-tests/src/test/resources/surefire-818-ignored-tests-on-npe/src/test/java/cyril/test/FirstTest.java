package cyril.test;


import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.junit.Test;


public class FirstTest
    extends MockObjectTestCase
{

    private Mock myServiceMock;

    @Override
    protected void setUp()
        throws Exception
    {
        myServiceMock = mock( MyService.class );
    }


    @Test
    public void test()
    {

        Message myMessage = new Message( "MyMessage" );
        // Expectations
        myServiceMock.expects( once() ).method( "writeMessage" ).with( eq( myMessage ) ).will(
            returnValue( myMessage ) );

        ( (MyService) myServiceMock.proxy() ).writeMessage( null );

    }

}
