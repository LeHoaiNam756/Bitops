package core.testpath;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.List;
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
            TraceRecorder.mark(11, TraceKind.COND_F);

            assertEquals(Set.of(7, 9, 11), TraceRecorder.coveredNodeIdsSnapshot());
            assertEquals(Set.of(7), TraceRecorder.coveredStatementNodeIdsSnapshot());
            assertEquals(Set.of(9, 11), TraceRecorder.coveredBranchOutcomeIdsSnapshot());
            assertEquals(List.of(
                    new TraceRecorder.OrderedTraceEvent(7, TraceKind.NODE),
                    new TraceRecorder.OrderedTraceEvent(9, TraceKind.COND_T),
                    new TraceRecorder.OrderedTraceEvent(9, TraceKind.COND_T),
                    new TraceRecorder.OrderedTraceEvent(11, TraceKind.COND_F)
            ), TraceRecorder.orderedEventsSnapshot());
        } finally {
            TraceRecorder.endSession();
        }

        assertTrue(TraceRecorder.coveredNodeIdsSnapshot().isEmpty());
        assertTrue(TraceRecorder.orderedEventsSnapshot().isEmpty());
    }
}
