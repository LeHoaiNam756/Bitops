public class LogicFlow {

    public int processValue(int input, int threshold, boolean scaleUp) {
        int result = 0;

        if (input < 0) {
            result = -1;
        } else if (input == 0) {
            result = 0;
        } else if (input > threshold) {
            int temp = input;
            while (temp > threshold) {
                temp -= 2;
                result++;
            }
        } else {
            for (int i = 0; i < input; i++) {
                result += i;
            }
        }

        int counter = 0;
        do {
            if (scaleUp) {
                result += 10;
            } else {
                result -= 5;
            }
            counter++;
        } while (counter < 3);

        return result;
    }

    /**
     * New method using mixed conditional logic (AND, OR, NOT)
     * nested within loops and branches.
     */
    public int evaluateComplexLogic(int alpha, int beta, boolean isActive) {
        int score = 0;

        // Mixed condition using AND (&&) and NOT (!)
        if (isActive && !(alpha < 0)) {

            // Loop with a mixed condition termination
            for (int i = 0; i < alpha || i < beta; i++) {

                // Nested multi-branch with mixed conditions
                if (i % 2 == 0 && i < 10) {
                    score += 2;
                } else if (i == beta || i > 20) {
                    score += 5;
                } else {
                    score += 1;
                }

                // Breaking logic based on mixed state
                if (score > 100 && beta > 0) {
                    break;
                }
            }
        } else if (!isActive || beta < 0) {
            score = -1;
        }

        return score;
    }

    /**
     * Demonstrates nested loops using primitive parameters.
     * Simulates a grid-based calculation where 'width' and 'height'
     * determine the iterations of inner and outer loops.
     */
    public int calculateGridImpact(int width, int height, int intensity) {
        int totalImpact = 0;
        int row = 0;

        // Outer loop: Processes "rows"
        while (row < height) {

            // Multi-branch logic inside the outer loop
            if (intensity > 0) {

                // Inner loop: Processes "columns" for each row
                for (int col = 0; col < width; col++) {

                    // Mixed condition inside nested loops
                    if ((row + col) % 2 == 0 || intensity > 10) {
                        totalImpact += intensity;
                    } else {
                        totalImpact += 1;
                    }
                }
            } else {
                totalImpact -= 1;
            }

            row++;
        }

        return totalImpact;
    }
}