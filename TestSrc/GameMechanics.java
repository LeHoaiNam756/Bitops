public class GameMechanics {

    /**
     * Simulates a turn-based combat sequence to calculate total damage dealt.
     */
    public int simulateBattleLog(int heroStrength, int heroStamina, int enemyHp, int enemyArmor, boolean isBossMode) {

        int totalDamageDealt = 0;
        int turnCount = 1;

        while (enemyHp > 0 && heroStamina > 0) {

            int turnDamageMultiplier;

            if (heroStamina >= 80) {
                turnDamageMultiplier = 3;
            } else if (heroStamina >= 40) {
                turnDamageMultiplier = 2;
            } else {
                turnDamageMultiplier = 1;
            }

            if (isBossMode) {
                if (turnCount % 4 == 0) {
                    enemyArmor = enemyArmor + 5;
                }
            }

            for (int hit = 1; hit <= 3; hit++) {

                if (heroStamina < 5) {
                    break;
                }

                int rawDamage = heroStrength * turnDamageMultiplier;

                if (hit == 2) {
                    rawDamage = rawDamage + (heroStrength / 2);
                } else if (hit == 3) {
                    rawDamage = rawDamage * 2;
                }

                int actualHit = rawDamage - enemyArmor;

                if (actualHit < 0) {
                    actualHit = 0;
                }

                enemyHp = enemyHp - actualHit;
                totalDamageDealt = totalDamageDealt + actualHit;

                heroStamina = heroStamina - 5;
            }


            int recoveryTicks = 0;
            int maxRecovery = 2;

            if (turnDamageMultiplier == 1) {
                maxRecovery = 5;
            }

            do {
                heroStamina = heroStamina + 2;


                if (enemyArmor > 0) {
                    if (turnCount % 2 != 0) {
                        enemyArmor = enemyArmor - 1;
                    }
                }

                recoveryTicks++;
            } while (recoveryTicks < maxRecovery && heroStamina < 100);

            turnCount++;
        }

        return totalDamageDealt;
    }
}