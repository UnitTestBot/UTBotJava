package org.utbot.examples.mixed;

public class StaticMethodExamples {
    public static boolean complement(int x) {
        return (~x) == 1;
    }

    public static int max2(int x, short y) {
        if (x > y) {
            return x;
        } else {
            return y;
        }
    }

    public static long sum(int x, short y, byte z) {
        int sum = x + y + z;
        if (sum > 20 || sum < -20) {
            return sum * 2L;
        }
        return sum;
    }
}
