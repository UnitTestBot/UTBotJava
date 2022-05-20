package org.utbot.examples.samples;

import java.util.Arrays;

public class ClassWithSameMethodNames {
    public static int sum(int a, int b) {
        return a + b + 2;
    }

    public static int sum(int a, int b, int c) {
        return a + b + c + 3;
    }

    public static int sum(int... values) {
        int len = values.length;
        if (len == 0) {
            return 0;
        }
        return values[len - 1] + sum(Arrays.copyOf(values, len - 1));
    }
}
