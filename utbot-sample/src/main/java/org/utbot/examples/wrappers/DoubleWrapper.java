package org.utbot.examples.wrappers;

public class DoubleWrapper {
    public Double primitiveToWrapper(double i) {
        Double a = i;
        if (a >= 0) {
            return a;
        } else {
            return -a;
        }
    }

    public double wrapperToPrimitive(Double i) {
        double a = i;
        if (a >= 0) {
            return a;
        } else {
            return -a;
        }
    }
}
