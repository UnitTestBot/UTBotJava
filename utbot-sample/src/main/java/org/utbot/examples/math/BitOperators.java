package org.utbot.examples.math;

public class BitOperators {
    public boolean complement(int x) {
        return (~x) == 1;
    }

    public boolean xor(int x, int y) {
        return (x ^ y) == 0;
    }

    public boolean or(int x) {
        return (x | 7) == 15; // x must have 1,2,3 bits as any, 4th bit set, all others unset
    }

    public boolean and(int x) {
        return (x & (x - 1)) == 0;
    }

    @SuppressWarnings("ConstantConditions")
    public int booleanNot(boolean a, boolean b) {
        boolean d = a && b;
        boolean e = !a || b;
        return d && e ? 100 : 200;
    }

    public boolean booleanXor(boolean a, boolean b) {
        return a ^ b;
    }

    public boolean booleanOr(boolean a, boolean b) {
        return a | b;
    }

    public boolean booleanAnd(boolean a, boolean b) {
        return a & b;
    }

    public int booleanXorCompare(boolean a, boolean b) {
        return (a ^ b) ? 1 : 0;
    }

    public boolean shl(int x) {
        return (x << 1) == 2;
    }

    public boolean shlLong(long x) {
        return (x << 1) == 2;
    }

    @SuppressWarnings("ShiftOutOfRange")
    public int shlWithBigLongShift(long shift) {
        if (shift < 40) {
            return 1;
        }
        return (0x77777777 << shift) == 0x77777770 ? 2 : 3;
    }

    public boolean shr(int x) {
        return (x >> 1) == 1;
    }

    public boolean shrLong(long x) {
        return (x >> 1) == 1;
    }

    public boolean ushr(int x) {
        return (x >>> 1) == 1;
    }

    public boolean ushrLong(long x) {
        return (x >>> 1) == 1;
    }

    @SuppressWarnings("UseCompareMethod")
    public int sign(int x) {
        // Integer::signum
        int i = (x >> 31) | (-x >>> 31);

        if (i > 0) {
            return 1;
        } else if (i == 0) {
            return 0;
        } else {
            return -1;
        }
    }
}