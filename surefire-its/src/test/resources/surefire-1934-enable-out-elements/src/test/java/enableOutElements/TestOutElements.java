package enableOutElements;

import org.apache.log4j.Logger;
import org.junit.Test;

public class TestOutElements {

    @Test
    public void successfulTestWithLogs() {
        Logger.getLogger(TestOutElements.class).info("Log output expected in test report.");
        System.out.println("System-out output should be in the report.");
        System.err.println("System-err output should be in the report.");
    }

}
