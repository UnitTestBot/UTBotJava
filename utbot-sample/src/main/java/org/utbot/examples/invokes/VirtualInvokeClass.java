package org.utbot.examples.invokes;

public class VirtualInvokeClass implements VirtualInvokeInterface<Integer, byte[]> {
    int foo(int value) {
        if (value > 0) {
            return 1;
        }
        if (value < 0) {
            return 2;
        }
        throw new RuntimeException();
    }

    int bar() {
        return 1;
    }

    int fooBar() {
        return bar();
    }

    Object getObject() {
        return 10;
    }

    @Override
    public final int narrowParameterTypeInInheritorObjectCast(Integer object) {
        if (object == null) {
            return 0;
        }

        if (object == 1) {
            return 1;
        }

        return object;
    }

    @Override
    public final int narrowParameterTypeInInheritorArrayCast(byte[] object) {
        if (object == null) {
            return 0;
        }

        return object[0];
    }
}

