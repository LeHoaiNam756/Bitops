public class WarehouseStock {

    /**
     * Simulates fulfilling multiple orders until stock is depleted.
     * Explores symbolic execution failure with loop variables reaching a zero
     * bound.
     */
    public int fulfillOrders(int totalStock, int standardPackageSize) {
        int fulfilledCount = 0;

        if (totalStock >= 10000) {
            if (standardPackageSize >= 100 && standardPackageSize <= 500) {

                // LOOP MUTATION
                while (totalStock >= standardPackageSize) {
                    totalStock = totalStock - standardPackageSize;
                    fulfilledCount++;

                    if (totalStock < 100) {
                        if (fulfilledCount > 20) {
                            System.out.println("Stock nearly exhausted after many orders.");
                        }
                    }
                }
            }
        }

        return fulfilledCount;
    }
}
