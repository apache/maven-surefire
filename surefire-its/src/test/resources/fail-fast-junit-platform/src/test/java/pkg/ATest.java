package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class ATest {
    @Test
    void test() throws Exception {
        MILLISECONDS.sleep(1_000);
        throw new RuntimeException();
    }
}
