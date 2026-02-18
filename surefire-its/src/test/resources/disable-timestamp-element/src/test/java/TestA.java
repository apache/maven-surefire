import org.junit.Test;
import org.junit.Ignore;

public class TestA {
    @Ignore("Skipping this test still has to report a processing event timestamp")
    @Test
    public void skipped() throws Exception {
        Thread.sleep(500);
    }

    @Test
    public void success() throws Exception {
        Thread.sleep(500);
    }

    @Test
    public void failure() throws Exception {
        Thread.sleep(500);
        throw new Exception("This test is supposed to fail.");
    }
}
