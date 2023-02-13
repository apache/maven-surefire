package junit4;
import org.junit.AfterClass;
import org.junit.Test;

public class NoCategoryTest {
  static int catNoneCount = 0;

  @Test
  public void testInNoCategory()
  {
      catNoneCount++;
  }

  @AfterClass
  public static void oneTimeTearDown()
  {
      System.out.println("NoCategoryTest.CatNone: " + catNoneCount);
  }
}
