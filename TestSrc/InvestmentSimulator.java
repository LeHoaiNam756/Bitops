public class InvestmentSimulator {

    /**
     * Calculates projected wealth based on savings, interest, and risk factors.
     *
     */
    public double calculateProjectedWealth(double principal, double monthlyContribution, int years, int riskLevel, boolean isTaxable) {

        if (years <= 0) {
            return principal;
        }
        if (riskLevel < 1) {
            riskLevel = 1;
        }
        if (riskLevel > 10) {
            riskLevel = 10;
        }

        double currentBalance = principal;
        int currentYear = 1;

        while (currentYear <= years) {

            double annualRate;
            if (riskLevel <= 2) {
                if (currentBalance < 10000) {
                    annualRate = 0.02;
                } else {
                    annualRate = 0.025;
                }
            } else if (riskLevel <= 5) {
                if (currentYear < 5) {
                    annualRate = 0.05;
                } else {
                    annualRate = 0.06;
                }
            } else if (riskLevel <= 8) {
                if (currentBalance > 50000) {
                    annualRate = 0.09;
                } else if (currentBalance > 20000) {
                    annualRate = 0.08;
                } else {
                    annualRate = 0.07;
                }
            } else {
                if (currentYear % 3 == 0) {
                    annualRate = -0.05;
                } else {
                    annualRate = 0.15;
                }
            }

            double monthlyRate = annualRate / 12.0;
            for (int month = 1; month <= 12; month++) {
                currentBalance = currentBalance + (currentBalance * monthlyRate);
                currentBalance = currentBalance + monthlyContribution;
            }

            if (isTaxable) {
                double profit = currentBalance - principal - (monthlyContribution * 12 * currentYear);

                if (profit > 0) {
                    double taxRate;
                    if (profit < 1000) {
                        taxRate = 0.0;
                    } else if (profit < 10000) {
                        taxRate = 0.10;
                    } else if (profit < 50000) {
                        taxRate = 0.15;
                    } else {
                        taxRate = 0.20;
                    }

                    double taxAmount = profit * taxRate;
                    currentBalance = currentBalance - taxAmount;
                }
            }

            double volatilityFactor = 0.001 * riskLevel;
            int stabilizationAttempts = 0;

            do {
                int temp = (int)currentBalance;
                if (temp % 2 == 0) {
                    currentBalance = currentBalance - (currentBalance * volatilityFactor);
                } else {
                    currentBalance = currentBalance + (currentBalance * volatilityFactor);
                }

                volatilityFactor = volatilityFactor / 2;
                stabilizationAttempts++;

            } while (stabilizationAttempts < 3 && volatilityFactor > 0.0001);

            currentYear++;
        }

        return currentBalance;
    }
}