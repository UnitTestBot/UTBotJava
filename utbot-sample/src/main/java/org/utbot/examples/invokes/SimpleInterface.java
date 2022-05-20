package org.utbot.examples.invokes;

public interface SimpleInterface {
    default int addConstantToValue(int value) {
        return value + 5;
    }

    default int subtractConstantFromValue(int value) {
        return value - 5;
    }

    int constValue();
}
