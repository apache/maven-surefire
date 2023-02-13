package jiras.surefire1036;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( IntegrationTest.class )
public class TestSomeIntegration
{
  @Test
  public void thisIsAnIntegrationTest() throws Exception
  {
    String message = "This integration test will always pass";
    System.out.println( message );
    assertTrue( message, true );
  }
}
