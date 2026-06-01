package core.generation;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

final class MemoryUsageMonitor {
    private static final com.sun.management.ThreadMXBean THREAD_MX_BEAN =
            threadMxBean();

    private MemoryUsageMonitor() {}

    static Snapshot capture() {
        return new Snapshot(
                currentThreadAllocatedBytes(),
                totalGcCollectionCount()
        );
    }

    static long allocatedBytesSince(Snapshot initialMemoryUsage) {
        long currentAllocatedBytes = currentThreadAllocatedBytes();
        if (initialMemoryUsage.threadAllocatedBytes() < 0L || currentAllocatedBytes < 0L) {
            return 0L;
        }

        long gcCollections = totalGcCollectionCount() - initialMemoryUsage.gcCollectionCount();
        if (gcCollections > 0L) {
            System.err.printf(
                    "[MemoryUsageMonitor] Detected %d GC collection(s) during memory measurement.%n",
                    gcCollections);
        }
        return Math.max(0L, currentAllocatedBytes - initialMemoryUsage.threadAllocatedBytes());
    }

    static long currentThreadAllocatedBytes() {
        if (THREAD_MX_BEAN == null || !THREAD_MX_BEAN.isThreadAllocatedMemorySupported()) {
            return -1L;
        }
        if (!THREAD_MX_BEAN.isThreadAllocatedMemoryEnabled()) {
            try {
                THREAD_MX_BEAN.setThreadAllocatedMemoryEnabled(true);
            } catch (SecurityException | UnsupportedOperationException e) {
                return -1L;
            }
        }
        return THREAD_MX_BEAN.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    static long totalGcCollectionCount() {
        long total = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            if (count >= 0L) {
                total += count;
            }
        }
        return total;
    }

    private static com.sun.management.ThreadMXBean threadMxBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean threadBean) {
            return threadBean;
        }
        return null;
    }

    record Snapshot(long threadAllocatedBytes, long gcCollectionCount) {}
}
