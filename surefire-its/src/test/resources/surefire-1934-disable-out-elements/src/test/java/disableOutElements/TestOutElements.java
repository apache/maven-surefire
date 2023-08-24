package disableOutElements;

import org.apache.log4j.Logger;
import org.junit.Test;

public class TestOutElements {

    @Test
    public void successfulTestWithLogs() {
        Logger.getLogger(TestOutElements.class).info("Log output not expected in test report.");
        System.out.println("System-out output should not be in the report.");
        System.err.println("System-err output should not be in the report.");
    }

}
