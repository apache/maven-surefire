package disableOutErrElements;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TestOutErrElements {

    @Test
    public void successfulTestWithLogs() {
        LoggerFactory.getLogger(TestOutErrElements.class).info("Log output not expected in test report.");
        System.out.println("System-out output not expected in the report.");
        System.err.println("System-err output not expected in the report.");
    }

    @Test
    public void failedTestWithLogs() throws Exception {
        LoggerFactory.getLogger(TestOutErrElements.class).info("Log output expected in test report.");
        System.out.println("System-out output expected in the report.");
        System.err.println("System-err output expected in the report.");
        throw new Exception("Expected to fail");
    }

}
