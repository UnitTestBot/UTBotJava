package org.utbot.examples.primitives;

public class FloatExamples {
    public int floatInfinity(float f) {
        if (f == Float.POSITIVE_INFINITY) {
            return 1;
        }
        if (f == Float.NEGATIVE_INFINITY) {
            return 2;
        }
        return 3;
    }
}