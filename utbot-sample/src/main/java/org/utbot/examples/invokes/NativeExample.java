package org.utbot.examples.invokes;

public class NativeExample {
    public int partialExecution(double x) {
        if (StrictMath.sin(x) > 0) {
            return 0;
        } else {
            return 1;
        }
    }

    public int unreachableNativeCall(double x) {
        if (x == x) {
            return 1;
        }
        if (x != x) {
            return 2;
        }
        // unreachable code:
        if (StrictMath.sin(x) > 0) {
            return 3;
        } else {
            return 4;
        }
    }

    public int substitution(double x) {
        double sqrt = Math.sqrt(x);

        if (sqrt > 2) {
            return 1;
        } else {
            return 0;
        }
    }

    public int unreachableBranch(double x) {
        if (Double.isNaN(x)) {
            return 1;
        }

        double log = Math.log(x);
        if (Double.isNaN(log) && x >= 0) {
            throw new RuntimeException("An unreachable branch, since we have NaN if x is NaN or x < 0");
        } else {
            return 2;
        }
    }
}
