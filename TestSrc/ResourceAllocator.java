public class ResourceAllocator {

    /**
     * Allocates resources based on an initial budget.
     * This example is designed to exploit the symbolic execution engine's
     * inability to correctly version mutated variables (lack of SSA form).
     */
    public int allocate(int budget, int serversCount) {
        int allocatedProcessingUnits = 0;

        if (budget >= 1000) {
            budget = budget - 800;
            allocatedProcessingUnits += 10;
        } else {
            // Path Constraint B: budget < 1000
            budget = budget - 100;
            allocatedProcessingUnits += 2;
        }

        if (budget < 300) {
            if (serversCount > 5) {
                allocatedProcessingUnits += 1;
            } else {
                allocatedProcessingUnits += 2;
            }
        }

        return allocatedProcessingUnits;
    }
}
