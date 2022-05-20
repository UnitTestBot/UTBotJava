package org.utbot.examples.samples.benchmark;

import java.math.BigInteger;

public class Fibonacci {
    public static BigInteger calc(int init0, int init1, int n) {
        assert (n >= 0);
        if (n == 0) {
            return BigInteger.valueOf(init0);
        }
        if (n == 1) {
            return BigInteger.valueOf(init1);
        }
        return calc(init0, init1, n - 1).add(calc(init0, init1, n - 2));
    }
}