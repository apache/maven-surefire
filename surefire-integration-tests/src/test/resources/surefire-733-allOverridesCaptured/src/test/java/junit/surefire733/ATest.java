package junit.surefire733;
import junit.framework.TestCase;


public class ATest
    extends TestCase
{
    public void testConsoleOut() {
        System.out.write( (int) 'a');
        final byte[] bytes = "bc".getBytes();
        System.out.write(bytes, 0, bytes.length);
        System.out.write('\n');
    }
}
