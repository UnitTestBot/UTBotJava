package org.utbot.examples.math;

import org.utbot.api.mock.UtMock;

public class DoubleFunctions {
    public double hypo(double a, double b) {
        UtMock.assume(1.0 < a && a < 10.0);
        UtMock.assume(1.0 < b && b < 10.0);

        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    public double max(double a, double b) {
        return a > b ? a : b;
    }

    public double circleSquare(double r) {
        if (r < 0 || Double.isNaN(r) || r > 10000) {
            throw new IllegalArgumentException();
        }
        double square = Math.PI * r * r;
        if (square > 777.85) {
            return square;
        } else {
            return 0;
        }
    }

    public int numberOfRootsInSquareFunction(double a, double b, double c) {
        UtMock.assume(!(Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)));

        double result = b * b - 4 * a * c;
        if (result > 0) {
            return 2;
        } else if (result == 0) {
            return 1;
        }
        return 0;
    }
}
