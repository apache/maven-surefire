package listener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * {@link TestExecutionListener} that writes a file when a test starts, whose existence can be checked
 * by surefire-integration.
 */
public class FileWritingTestExecutionListener implements TestExecutionListener {

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            writeFile("testexecutionlistener-output.txt", "TestExecutionListener#executionStarted()");
        }
    }

    private static void writeFile(String fileName, String content) {
        try {
            File target = new File("target").getAbsoluteFile();
            File listenerOutput = new File(target, fileName);
            try (FileWriter out = new FileWriter(listenerOutput)) {
                out.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
