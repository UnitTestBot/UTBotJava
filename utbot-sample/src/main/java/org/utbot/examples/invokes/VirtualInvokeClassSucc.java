package org.utbot.examples.invokes;

public class VirtualInvokeClassSucc extends VirtualInvokeClass {
    private int x;

    int returnX(VirtualInvokeClassSucc obj) {
        return obj.x;
    }

    @Override
    int foo(int value) {
        if (value > 0) {
            return 1;
        }
        if (value < 0) {
            return -1;
        }
        return 0;
    }

    @Override
    int bar() {
        return 2;
    }

    @Override
    int fooBar() {
        return bar();
    }

    @Override
    Object getObject() {
        return null;
    }
}