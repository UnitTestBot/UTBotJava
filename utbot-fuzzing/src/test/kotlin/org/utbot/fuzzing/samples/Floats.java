package org.utbot.fuzzing.samples;

@SuppressWarnings("unused")
public class Floats {

    // Should find the value between 0 and -1.
    public int floatToInt(float x) {
        if (x < 0) {
            if ((int) x < 0) {
                return 1;
            }
            return 2; // smth small to int zero
        }
        return 3;
    }

    // should find all branches that return -2, -1, 0, 1, 2.
    public int numberOfRootsInSquareFunction(double a, double b, double c) {
        if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)) return -1;
        if (a == 0.0 || b == 0.0 || c == 0.0) return -2;
        double result = b * b - 4 * a * c;
        if (result > 0) {
            return 2;
        } else if (result == 0) {
            return 1;
        }
        return 0;
    }

    // will never be succeeded to find the value because of floating precision
    public void floatEq(float v) {
        if (v == 28.7) {
            throw new IllegalArgumentException();
        }
    }

    // should generate double for constant float that equals to 28.700000762939453
    public void floatEq(double v) {
        if (v == 28.7f) {
            throw new IllegalArgumentException();
        }
    }

}
