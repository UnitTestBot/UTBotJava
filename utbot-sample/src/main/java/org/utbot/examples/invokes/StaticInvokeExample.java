package org.utbot.examples.invokes;

public class StaticInvokeExample {
    public static int maxForThree(int x, short y, byte z) {
        int max = maxForTwo(x, y);

        if (max > z) {
            return max;
        } else {
            return z;
        }
    }

    private static int maxForTwo(int x, short y) {
        if (x > y) {
            return x;
        } else {
            return y;
        }
    }
}
