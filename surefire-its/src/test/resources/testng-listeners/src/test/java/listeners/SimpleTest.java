package listeners;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Created by etigwuu on 2014-04-26.
 */
@Listeners(MarkAsFailureListener.class)
public class SimpleTest {

    @Test
    public void test1(){
        System.out.println("Hello world");
    }
}
