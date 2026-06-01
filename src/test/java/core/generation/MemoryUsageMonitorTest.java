package core.generation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MemoryUsageMonitorTest {

    @Test
    public void allocatedBytesSince_neverReturnsNegativeAllocationDelta() {
        MemoryUsageMonitor.Snapshot snapshot =
                new MemoryUsageMonitor.Snapshot(
                        Long.MAX_VALUE,
                        MemoryUsageMonitor.totalGcCollectionCount()
                );

        assertEquals(0L, MemoryUsageMonitor.allocatedBytesSince(snapshot));
    }

    @Test
    public void allocatedBytesSince_returnsZeroWhenThreadAllocationIsUnavailable() {
        MemoryUsageMonitor.Snapshot snapshot =
                new MemoryUsageMonitor.Snapshot(-1L, MemoryUsageMonitor.totalGcCollectionCount());

        assertEquals(0L, MemoryUsageMonitor.allocatedBytesSince(snapshot));
    }
}
