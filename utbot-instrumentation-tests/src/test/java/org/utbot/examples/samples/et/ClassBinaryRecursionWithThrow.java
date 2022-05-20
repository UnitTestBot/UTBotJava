package org.utbot.examples.samples.et;

public class ClassBinaryRecursionWithThrow {
    public static void A(int v, int d, boolean bad) throws Exception {
        if (d == 0) {
            return;
        }
        if (v == 1) {
            B(v * 2, d - 1, bad);
        } else {
            A(v * 2, d - 1, bad);
        }
        A(v * 2 + 1, d - 1, bad);
    }

    private static void B(int v, int d, boolean bad) throws Exception {
        if (d == 0) {
            if (bad) {
                throw new Exception();
            }
            return;
        }
        A(v * 2, d - 1, bad);
        try {
            B(v * 2 + 1, d - 1, bad);
        } catch (Exception e) {
            String s = "catched";
        }
    }
}

// #1. A(1, 2, false)
// #2. A(1, 2, true)
