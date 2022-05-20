package org.utbot.examples.samples.staticenvironment;

import org.jetbrains.annotations.Nullable;

import static java.lang.Math.max;

public class StaticExampleClass {
    public static int digit = 9;
    public static String digitS = "9";
    public static int[] arr = new int[20];

    public static int inc() {
        if (Integer.parseInt(digitS) != digit) {
            return 1;
        }

        digit++;
        if (digit == 10) {
            digit = 0;
        }

        digitS = String.valueOf(digit);
        return 0;
    }

    public static int plus(int times) {
        int ok = 0;
        for (int i = 0; i < times; i++) {
            ok = max(0, inc());
        }
        arr[5] = 10;
        return ok;
    }

    @Nullable
    public static String canBeNull(int value, @Nullable String str) {
        return value == 0 ? null : str;
    }

    public static String canBeNullWithoutAnnotations(int value, String str) {
        return value == 0 ? null : str;
    }
}
