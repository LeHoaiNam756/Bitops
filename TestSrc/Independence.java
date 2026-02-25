public class Independence {
    public static int independentBranches(int x1, int x2, int x3, int x4) {

        int result = 0;

        // Branch 1 (independent)
        if (x1 > 0) {
            result += 1;
            System.out.println("Branch 1: TRUE");
        } else {
            result += 2;
            System.out.println("Branch 1: FALSE");
        }

        // Branch 2 (independent)
        if (x2 > 0) {
            result += 4;
            System.out.println("Branch 2: TRUE");
        } else {
            result += 8;
            System.out.println("Branch 2: FALSE");
        }

        // Branch 3 (independent)
        if (x3 > 0) {
            result += 16;
            System.out.println("Branch 3: TRUE");
        } else {
            result += 32;
            System.out.println("Branch 3: FALSE");
        }

        // Branch 4 (independent)
        if (x4 > 0) {
            result += 64;
            System.out.println("Branch 4: TRUE");
        } else {
            result += 128;
            System.out.println("Branch 4: FALSE");
        }

        System.out.println("Result = " + result);
        System.out.println("----------------------");

        return result;
    }

    public int loop(int n, int m) {
        int sum = 0;
        if (n < 0) {
            return -1; // Invalid input
        }
        if (m < 0) {
            return -2; // Invalid input
        }

        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                sum += i; // Add even numbers
            } else {
                sum -= i; // Subtract odd numbers
            }
        }
        return sum;
    }
}