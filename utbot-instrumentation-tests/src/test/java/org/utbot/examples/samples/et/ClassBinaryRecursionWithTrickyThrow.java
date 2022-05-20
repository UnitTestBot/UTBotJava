package org.utbot.examples.samples.et;

public class ClassBinaryRecursionWithTrickyThrow {
    public static int A(int l, int r, int x) {
        if (x == 0) {
            return (r - l);
        }
        int m = (l + r) / 2;
        return A(l, m, x - 1) + A(m, r, x - 1);
    }

    public static int A_catchesAll(boolean left, int y) {
        if (y == 0) {
            throw (left ? new IllegalArgumentException() : new ArithmeticException());
        }
        if (left) {
            int a = -1;
            try {
                a = A_catchesAll(true, y - 1);

            } catch (Exception exc) {
                String s = "Catched!";
            }
            int b = -1;
            try {
                b = A_catchesAll(false, y - 1);
            } catch (Exception exc) {
                String s = "Catched!";
            }
            return a + b;
        } else {
            int a = A_catchesAll(true, y - 1);
            int b = A_catchesAll(false, y - 1);
            return a + b;
        }
    }

    public static void A_notAll(boolean left, int y) {
        if (y == 0) {
            throw (left ? new IllegalArgumentException() : new ArithmeticException());
        }
        if (y == 1) {
            try {
                A_notAll(!left, 0);
            } catch (IllegalArgumentException exc) {
                String s = "Catched!";
            }

            try {
                A_notAll(left, 0);
            } catch (IllegalArgumentException exc) {
                String s = "Catched!";
            }
        } else {
            try {
                A_notAll(true, y - 1);

            } catch (ArithmeticException exc) {
                String s = "Catched!";
            }

            try {
                A_notAll(false, y - 1);
            } catch (ArithmeticException exc) {
                String s = "Catched!";
            }
        }
    }
}

// #1. A(1, 10, 2)
// #2. A_catchesAll(true, 2)
// #3. A_notAll(false, 2)