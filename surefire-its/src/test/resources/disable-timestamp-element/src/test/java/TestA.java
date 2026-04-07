import org.junit.Test;
import org.junit.Ignore;

public class TestA {
    @Ignore("Skipping this test still has to report a processing event timestamp")
    @Test
    public void skipped() throws Exception {
    }

    @Test
    public void success() throws Exception {
    }

    @Test
    public void failure() throws Exception {
        throw new Exception("This test is supposed to fail.");
    }
}
