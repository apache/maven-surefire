package runorder.parallel;

import org.junit.Test;
public class Test1
{
  
    static void sleep(int ms){
      try {
        Thread.sleep(ms);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

   @Test
    public void testSleep200() {
     System.out.println("Test1.sleep200 started @ " + System.currentTimeMillis());
        sleep(200);
    }

  @Test
   public void testSleep400() {
    System.out.println("Test1.sleep400 started @ " + System.currentTimeMillis());
       sleep(400);
   }

  @Test
   public void testSleep600() {
    System.out.println("Test1.sleep600 started @ " + System.currentTimeMillis());
       sleep(600);
   }

}