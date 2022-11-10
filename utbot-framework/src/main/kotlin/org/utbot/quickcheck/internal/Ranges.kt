

package org.utbot.quickcheck.internal;

import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigInteger;

import static java.lang.String.format;

public final class Ranges {
    public enum Type {
        CHARACTER("c"),
        INTEGRAL("d"),
        FLOAT("f"),
        STRING("s");

        private final String pattern;

        Type(String pattern) {
            this.pattern = pattern;
        }
    }

    private Ranges() {
        throw new UnsupportedOperationException();
    }

    public static <T extends Comparable<? super T>>
    int checkRange(Type type, T min, T max) {
        int comparison = min.compareTo(max);
        if (comparison > 0) {
            throw new IllegalArgumentException(
                format(
                    "bad range, %" + type.pattern + " > %" + type.pattern,
                    min,
                    max));
        }

        return comparison;
    }

    public static BigInteger choose(
        SourceOfRandomness random,
        BigInteger min,
        BigInteger max) {

        BigInteger range = max.subtract(min).add(BigInteger.ONE);
        BigInteger generated;

        do {
            generated = random.nextBigInteger(range.bitLength());
        } while (generated.compareTo(range) >= 0);

        return generated.add(min);
    }

    public static long choose(SourceOfRandomness random, long min, long max) {
        checkRange(Type.INTEGRAL, min, max);

        /* There are some edges cases with integer overflows, for instance,
           when (max - min) exceeds Long.MAX_VALUE. These cases should be
           relatively rare under the assumption that choosing
           [Long.MIN_VALUE, Long.MAX_VALUE] can be simplified to choosing any
           random long. Thus, the optimization here only deals with the common
           situation that no overflows are possible (maybe the heuristic to
           detect that could be improved).
         */
        boolean noOverflowIssues =
            max < ((long) 1 << 62) && min > -(((long) 1) << 62);

        if (noOverflowIssues) {
            // fast path: use long computations
            long range = (max - min) + 1;
            long mask = findNextPowerOfTwoLong(range) - 1;

            // loop to avoid distribution bias (as would be the case
            // with modulo division)
            long generated;
            do {
                generated = Math.abs(random.nextLong()) & mask;
            } while (generated >= range);

            return generated + min;
        } else {
            // slow path: fall back to BigInteger to avoid any surprises
            return choose(
                random,
                BigInteger.valueOf(min),
                BigInteger.valueOf(max))
                .longValue();
        }
    }

    static long findNextPowerOfTwoLong(long positiveLong) {
        return isPowerOfTwoLong(positiveLong)
            ? positiveLong
            : ((long) 1) << (64 - Long.numberOfLeadingZeros(positiveLong));
    }

    private static boolean isPowerOfTwoLong(long positiveLong) {
        return (positiveLong & (positiveLong - 1)) == 0;
    }
}
