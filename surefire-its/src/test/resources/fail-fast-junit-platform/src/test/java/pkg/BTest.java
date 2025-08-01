package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class BTest {
    @Test
    void test() throws Exception {
        MILLISECONDS.sleep(2_000);
        throw new RuntimeException();
    }
}
