package org.utbot.examples.arrays;

public class FinalStaticFieldArray {
    static double checkNonNegative(String role, double x) {
        if (!(x >= 0)) { // not x < 0, to work with NaN.
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    //This is exact example from Guava where static final array with initialization is presented
    public static double factorial(int n) {
        checkNonNegative("n", n);
        if (n > MAX_FACTORIAL) {
            return Double.POSITIVE_INFINITY;
        } else {
            // Multiplying the last (n & 0xf) values into their own accumulator gives a more accurate
            // result than multiplying by everySixteenthFactorial[n >> 4] directly.
            double accum = 1.0;
            for (int i = 1 + (n & ~0xf); i <= n; i++) {
                accum *= i;
            }
            return accum * everySixteenthFactorial[n >> 4];
        }
    }

    static final int MAX_FACTORIAL = 170;

    static final double[] everySixteenthFactorial = {
            0x1.0p0,
            0x1.30777758p44,
            0x1.956ad0aae33a4p117,
            0x1.ee69a78d72cb6p202,
            0x1.fe478ee34844ap295,
            0x1.c619094edabffp394,
            0x1.3638dd7bd6347p498,
            0x1.7cac197cfe503p605,
            0x1.1e5dfc140e1e5p716,
            0x1.8ce85fadb707ep829,
            0x1.95d5f3d928edep945
    };
}
