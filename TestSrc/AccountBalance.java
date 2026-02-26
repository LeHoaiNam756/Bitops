public class AccountBalance {

    /**
     * Simulates withdrawals and deposits.
     * Explores symbolic execution failure with sequential state mutation.
     */
    public boolean processTransaction(int initialBalance, int withdrawalAmount, int depositAmount) {
        int balance = initialBalance;
        boolean success = false;

        // Condition 1: High balance required initially
        if (balance > 5000) {

            // MUTATION 1: Large withdrawal
            balance = balance - withdrawalAmount;

            // In a real execution, if initialBalance = 6000 and withdrawalAmount = 5500,
            // balance is now 500.

            // Condition 2: Check for low balance
            // Flawed Engine will combine: (balance > 5000) AND (balance < 1000) =>
            // UNSATISFIABLE
            if (balance < 1000) {

                // MUTATION 2: Make a deposit
                balance = balance + depositAmount;

                // Condition 3: Check for high balance again
                // Flawed Engine will combine: (balance > 5000) AND (balance < 1000) AND
                // (balance > 2000)
                // Which is completely broken.
                if (balance > 2000) {
                    success = true;
                    System.out.println("Emergency funds deposited successfully.");
                }
            } else {
                success = true; // Normal transaction
            }
        }

        return success;
    }
}
