package guava.examples.math;

import java.math.RoundingMode;

public class IntMath {
    /**
     * The biggest half power of two that can fit in an unsigned int.
     */
    static final int MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333;

    public static int log2(int x, RoundingMode mode) {
        switch (mode) {
            case UNNECESSARY:
                // fall through
            case DOWN:
            case FLOOR:
                return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Integer.SIZE - Integer.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                int leadingZeros = Integer.numberOfLeadingZeros(x);
                int cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                int logFloor = (Integer.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError();
        }
    }

    static int lessThanBranchFree(int x, int y) {
        // The double negation is optimized away by normal Java, but is necessary for GWT
        // to make sure bit twiddling works as expected.
        return ~~(x - y) >>> (Integer.SIZE - 1);
    }

    public static int pow(int b, int k) {
        switch (b) {
            case 0:
                return (k == 0) ? 1 : 0;
            case 1:
                return 1;
            case (-1):
                return ((k & 1) == 0) ? 1 : -1;
            case 2:
                return (k < Integer.SIZE) ? (1 << k) : 0;
            case (-2):
                if (k < Integer.SIZE) {
                    return ((k & 1) == 0) ? (1 << k) : -(1 << k);
                } else {
                    return 0;
                }
            default:
                // continue below to handle the general case
        }
        for (int accum = 1; ; k >>= 1) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return b * accum;
                default:
                    accum *= ((k & 1) == 0) ? 1 : b;
                    b *= b;
            }
        }
    }
}
