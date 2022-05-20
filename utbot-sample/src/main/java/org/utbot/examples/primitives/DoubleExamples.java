package org.utbot.examples.primitives;

public class DoubleExamples {
    public double compareSum(double a, double b) {
        double z = a + b;
        if (z > 5.6) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public double compare(double a, double b) {
        if (a > b) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public double compareWithDiv(double a, double b) {
        double z = a + 0.5;
        if ((a / z) > b) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public double simpleSum(double a, double b) {
        if (Double.isNaN(a + b)) {
            return 0.0;
        }
        double c = a + 1.1;
        if (b + c > 10.1 && b + c < 11.125) {
            return 1.1;
        } else {
            return 1.2;
        }
    }

    public double sum(double a, double b) {
        if (Double.isNaN(a + b)) {
            return 0.0;
        }
        double c = a + 0.123124;
        if (b + c > 11.123124 && b + c < 11.125) {
            return 1.1;
        } else {
            return 1.2;
        }
    }

    public double simpleMul(double a, double b) {
        if (Double.isNaN(a * b)) {
            return 0;
        }
        if (a * b > 33.1 && a * b < 33.875) {
            return 1.1;
        } else {
            return 1.2;
        }
    }

    public double mul(double a, double b) {
        if (Double.isNaN(a * b)) {
            return 0;
        }
        if (a * b > 33.32 && a * b < 33.333) {
            return 1.1;
        } else if (a * b > 33.333 && a * b < 33.7592) {
            return 1.2;
        } else {
            return 1.3;
        }
    }

    public double checkNonInteger(double a) {
        if (a > 0.1 && a < 0.9) {
            return 1.0;
        }
        return 0.0;
    }

    public double div(double a, double b, double c) {
        return (a + b) / c;
    }

    public int simpleEquation(double a) {
        if (a + a + a - 9 == a + 3) {
            return 0;
        } else {
            return 1;
        }
    }

    public int simpleNonLinearEquation(double a) {
        if (3 * a - 9 == a + 3) {
            return 0;
        } else {
            return 1;
        }
    }

    public int checkNaN(double d) {
        if (d < 0) {
            return -1;
        }
        if (d > 0) {
            return 1;
        }
        if (d == 0) {
            return 0;
        }
        // NaN
        return 100;
    }

    public int unaryMinus(double d) {
        if (-d < 0) {
            return -1;
        }
        return 0;
    }

    public int doubleInfinity(double d) {
        if (d == Double.POSITIVE_INFINITY) {
            return 1;
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return 2;
        }
        return 3;
    }
}