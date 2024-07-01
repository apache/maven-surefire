package disablePropertiesElement;

import org.junit.Test;

public class TestPropertiesElement {

    @Test
    public void success() {
        System.out.println("This is successful.");
    }

    @Test
    public void failure() throws Exception {
        System.out.println("This is faulty.");
        throw new Exception("Expected to fail");
    }

}
