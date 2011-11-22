package runorder.parallel;

import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class Test2 {

  @Test
  public void testSleep100() {
    System.out.println("Test2.sleep100 started @ " + System.currentTimeMillis());
    Test1.sleep(100);
  }

  @Test
  public void testSleep300() {
    System.out.println("Test2.sleep300 started @ " + System.currentTimeMillis());
    Test1.sleep(300);
  }

  @Test
  public void testSleep500() {
    System.out.println("Test2.sleep500 started @ " + System.currentTimeMillis());
    Test1.sleep(500);
  }
}