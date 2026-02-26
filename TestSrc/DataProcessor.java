public class DataProcessor {

    /**
     * Processes batches of data records.
     * Explores the engine's limitations when dealing with variable mutation inside
     * loops.
     */
    public int process(int records, int baseBatchSize) {
        int processedCount = 0;
        int currentBatchSize = baseBatchSize;

        if (records > 500) {
            currentBatchSize = 50;
        } else {
            if (records <= 0) {
                return 0;
            }
            currentBatchSize = 10;
        }

        // Loop entry
        while (records > 0) {
            records = records - currentBatchSize;
            processedCount += currentBatchSize;

            if (records < 20) {
                currentBatchSize = 1;

                if (processedCount > 400) {
                    System.out.println("Processing final trailing records.");
                }
            }
        }

        return processedCount;
    }
}
