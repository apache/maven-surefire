package testng;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

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
