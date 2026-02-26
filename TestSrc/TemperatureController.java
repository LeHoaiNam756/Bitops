public class TemperatureController {

    /**
     * Simulates a cooling process stepping down temperature.
     * Explores symbolic execution failure with incremental reassignment.
     */
    public int coolDown(int currentTemp, int targetTemp) {
        int stepsTaken = 0;
        int maxSteps = 10;

        if (currentTemp > 100) {
            if (targetTemp < 30) {
                for (int i = 0; i < maxSteps; i++) {
                    currentTemp = currentTemp - 10;
                    stepsTaken++;

                    if (currentTemp < 50) {
                        if (currentTemp <= targetTemp) {
                            System.out.println("Target reached.");
                            break;
                        }
                    }
                }

                if (currentTemp > 80 && stepsTaken == maxSteps) {
                    return -1; // Cooling failed
                }
            }
        }

        return currentTemp;
    }
}
