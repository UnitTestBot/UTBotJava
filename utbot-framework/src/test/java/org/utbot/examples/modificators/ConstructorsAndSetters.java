package org.utbot.examples.modificators;

/**
 * A class with constructors, setters and methods that look like setters.
 */
public class ConstructorsAndSetters {
    protected int i;

    private double d1, d2;
    private boolean b1, b2;

    public void setI(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("i");
        }
        this.i = i;
    }

    public void setInSetterLikeMethodWithoutArgs() {
        this.d1 = 1.0;
    }

    public void setInSetterLikeMethodWithMultipleArgs(double d1, double d2) {
        this.d1 = d1;
        this.d2 = d2;
    }

    public void setWithInternalCall(int value) {
        d1 = sqr(value);
    }

    public void setMultipleWithInternalCall(int value) {
        d1 = sqr(value);
        d2 = value;
    }

    public void setWithModifyingInternalCall(double value) {
        sqrIntoD1(value);
        d2 = value;
    }

    public ConstructorsAndSetters(double d1, double d2) {
        this.d1 = d1;
        this.d2 = d2;
    }

    public void createBoolFields(boolean b1, boolean b2) {
        this.b1 = !b1;
        this.b2 = !b2;
    }

    private int sqr(int value) {
        return value * value;
    }

    private void sqrIntoD1(double value) {
        d1 = value * value;
    }
}
