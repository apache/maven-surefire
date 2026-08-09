package surefire2113;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TempDirTest {
    @Test
    void tempDirUsesJavaIoTmpDir(@TempDir Path tempDir) throws IOException {
        Path expected = Paths.get(
                        System.getProperty("expected.java.io.tmpdir", System.getProperty("java.io.tmpdir")))
                .toRealPath();
        Path configured = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
        Path actual = tempDir.getParent().toRealPath();

        assertEquals(expected, configured);
        assertEquals(expected, actual);
    }
}
