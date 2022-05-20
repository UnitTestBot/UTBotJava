package org.utbot.examples.invokes;

public class SimpleInterfaceExample {
    public int overrideMethod(SimpleInterface objectExample, int value) {
        return objectExample.addConstantToValue(value);
    }

    public int defaultMethod(SimpleInterface objectExample, int value) {
        return objectExample.subtractConstantFromValue(value);
    }

    public int invokeMethodFromImplementor(AbstractImplementor objectExample) {
        return objectExample.constValue();
    }
}
