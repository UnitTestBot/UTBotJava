package org.utbot.examples.invokes;

@SuppressWarnings("unused")
public class SimpleInterfaceImpl implements SimpleInterface {
    public int addConstantToValue(int value) {
        return value + 2;
    }

    @Override
    public int constValue() {
        return 5;
    }
}
