package org.utbot.fuzzing.samples;

@SuppressWarnings({"unused", "RedundantIfStatement"})
public class Integers {

    public byte biting(byte a, byte b, boolean s) {
        if (s) {
            return a;
        } else {
            return b;
        }
    }

    // should cover 100%
    public static void diff(int a) {
        a = Math.abs(a);
        while (a > 0) {
            a = a % 2;
        }
        if (a < 0) {
            throw new IllegalArgumentException();
        }
        throw new RuntimeException();
    }

    // should cover 100% and better when values are close to constants,
    // also should generate "this" empty object
    public String extent(int a) {
        if (a < -2.0) {
            return "-1";
        }
        if (a > 5) {
            return "-2";
        }
        if (a == 3) {
            return "-3";
        }
        if (4L < a) {
            return "-4";
        }
        return "0";
    }

    // should cover 100% with 3 tests
    public static boolean isGreater(long a, short b, int c) {
        if (b > a && a < c) {
            return true;
        }
        return false;
    }

    // should find a bad value with integer overflow
    public boolean unreachable(int x) {
        int y = x * x - 2 * x + 1;
        if (y < 0) throw new IllegalArgumentException();
        return true;
    }

    public boolean chars(char a) {
        if (a >= 'a' && a <= 'z') {
            return true;
        }
        return false;
    }

}
