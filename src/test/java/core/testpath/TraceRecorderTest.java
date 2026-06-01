package core.testpath;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;

public class TraceRecorderTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test(expected = IllegalStateException.class)
    public void startSessionFailsWhenTraceDirectoryCannotBeCreated() throws IOException {
        Path fileInsteadOfDirectory = temp.newFile("clone").toPath();

        TraceRecorder.startSession("sample.Calculator#add", fileInsteadOfDirectory);
    }
}
