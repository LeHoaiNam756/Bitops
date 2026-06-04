package core.testpath;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import core.instrument.TraceKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraceRecorderTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test(expected = IllegalStateException.class)
    public void startSessionFailsWhenTraceDirectoryCannotBeCreated() throws IOException {
        Path fileInsteadOfDirectory = temp.newFile("clone").toPath();

        TraceRecorder.startSession("sample.Calculator#add", fileInsteadOfDirectory);
    }

    @Test
    public void coveredNodeIdsSnapshotReturnsHitsBeforeSessionEnds() throws IOException {
        Path clonedRoot = temp.newFolder("clone-root").toPath();

        TraceRecorder.startSession("sample.Calculator#add", clonedRoot);
        try {
            TraceRecorder.mark(7, TraceKind.NODE);
            TraceRecorder.mark(9, TraceKind.COND_T);
            TraceRecorder.mark(9, TraceKind.COND_T);

            assertEquals(Set.of(7, 9), TraceRecorder.coveredNodeIdsSnapshot());
        } finally {
            TraceRecorder.endSession();
        }

        assertTrue(TraceRecorder.coveredNodeIdsSnapshot().isEmpty());
    }
}
