package org.utbot.examples.math;

public class DivRemExamples {
    public int div(int x, int y) {
        return x / y;
    }

    public int rem(int x, int y) {
        return x % y;
    }

    public boolean remPositiveConditional(int d) {
        return (11 % d == 2);
    }

    public boolean remNegativeConditional(int d) {
        return (-11 % d == -2);
    }


    public boolean remWithConditions(int d) {
        return d >= 0 && (-11 % d == -2);
    }

    public double remDoubles(double x, double y) {
        return x % y;
    }

    public double remDoubleInt(double x, int y) {
        return x % y;
    }
}
