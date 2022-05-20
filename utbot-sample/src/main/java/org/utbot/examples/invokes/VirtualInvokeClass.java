package org.utbot.examples.invokes;

public class VirtualInvokeClass {
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
}

