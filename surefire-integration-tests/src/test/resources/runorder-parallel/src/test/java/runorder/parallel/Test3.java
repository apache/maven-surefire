package runorder.parallel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class Test3 {

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSleep10() {
    System.out.println("Test3.sleep10 started @ " + System.currentTimeMillis());
    Test1.sleep(10);
  }

  @Test
  public void testSleep30() {
    System.out.println("Test3.sleep30 started @ " + System.currentTimeMillis());
    Test1.sleep(30);
  }

  @Test
  public void testSleep50() {
    System.out.println("Test3.sleep50 started @ " + System.currentTimeMillis());
    Test1.sleep(50);
  }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        System.out.println( Thread.currentThread().getName() + " Test3 beforeClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        System.out.println( Thread.currentThread().getName() + " Test3 afterClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

}