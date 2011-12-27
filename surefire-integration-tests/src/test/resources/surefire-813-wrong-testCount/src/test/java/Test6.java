import org.junit.*;

public class Test6 {
  @Test
  public void test61() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
    aPrint(); // Does not fail if we call to sPrint() instead of aPrint() here
  }

  private static final String s = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

  public static void sPrint(){
    for (int i=0; i<1000; ++i){ // Increase this number if it does no fail
      System.out.println(i+":"+s);
      //System.err.println(i+":"+s);   //Fails as well is you print in err instead of out
    }
  }

  public static void aPrint() {
    new MyThread().start();
  }

  private static class MyThread extends Thread {
    @Override
    public void run() {
      sPrint();
    }
  }
}
