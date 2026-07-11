public class LongMath {
    public long pow(long b, int k) {
        if (k < 0) {
            throw new IllegalArgumentException("exponent (" + k + ") must be >= 0");
        }
        if (-2 <= b && b <= 2) {
        switch ((int) b) {
            case 0:
            return (k == 0) ? 1 : 0;
            case 1:
            return 1;
            case -1:
            return ((k & 1) == 0) ? 1 : -1;
            case 2:
            return (k < Long.SIZE) ? 1L << k : 0;
            case -2:
            if (k < Long.SIZE) {
                return ((k & 1) == 0) ? 1L << k : -(1L << k);
            } else {
                return 0;
            }
            default:
            throw new AssertionError();
        }
        }
        for (long accum = 1; ; k >>= 1) {
        switch (k) {
            case 0:
            return accum;
            case 1:
            return accum * b;
            default:
            accum *= ((k & 1) == 0) ? 1 : b;
            b *= b;
        }
        }
    }



    public long gcd(long a, long b) {
        /*
        * The reason we require both arguments to be >= 0 is because otherwise, what do you return on
        * gcd(0, Long.MIN_VALUE)? BigInteger.gcd would return positive 2^63, but positive 2^63 isn't an
        * int.
        */
        if (a < 0 || b < 0) {
            throw new IllegalArgumentException("Both arguments to gcd must be >= 0");
        }
        if (a == 0) {
        // 0 % b == 0, so b divides a, but the converse doesn't hold.
        // BigInteger.gcd is consistent with this decision.
        return b;
        } else if (b == 0) {
        return a; // similar logic
        }
        /*
        * Uses the binary GCD algorithm; see http://en.wikipedia.org/wiki/Binary_GCD_algorithm. This is
        * >60% faster than the Euclidean algorithm in benchmarks.
        */
        int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
        // The key to the binary GCD algorithm is as follows:
        // Both a and b are odd. Assume a > b; then gcd(a - b, b) = gcd(a, b).
        // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

        // We bend over backwards to avoid branching, adapting a technique from
        // http://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

        long delta = a - b; // can't overflow, since a and b are nonnegative

        long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
        // equivalent to Math.min(delta, 0)

        a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
        // a is now nonnegative and even

        b += minDeltaOrZero; // sets b to min(old a, b)
        a >>= Long.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << Math.min(aTwos, bTwos);
    }

    public long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
        // If a and b have different signs or a has the same sign as the result then there was no
        // overflow, return.
        return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    public long saturatedSubtract(long a, long b) {
        long naiveDifference = a - b;
        if ((a ^ b) >= 0 | (a ^ naiveDifference) >= 0) {
        // If a and b have the same signs or a has the same sign as the result then there was no
        // overflow, return.
        return naiveDifference;
        }
        // we did over/under flow
        return Long.MAX_VALUE + ((naiveDifference >>> (Long.SIZE - 1)) ^ 1);
    }


    public long saturatedMultiply(long a, long b) {
        // see checkedMultiply for explanation
        int leadingZeros =
            Long.numberOfLeadingZeros(a)
                + Long.numberOfLeadingZeros(~a)
                + Long.numberOfLeadingZeros(b)
                + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        // the return value if we will overflow (which we calculate by overflowing a long :) )
        long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
        if (leadingZeros < Long.SIZE | (a < 0 & b == Long.MIN_VALUE)) {
        // overflow
            return limit;
        }
        long result = a * b;
        if (a == 0 || result / a == b) {
            return result;
        }
        return limit;
    }
}
