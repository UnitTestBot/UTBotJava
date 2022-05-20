package org.utbot.examples.invokes;

public class InvokeClass {
    public int value;

    public int divBy(int den) {
        return value / den;
    }

    public void updateValue(int newValue) {
        value = newValue;
    }
}
