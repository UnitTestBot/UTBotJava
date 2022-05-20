package org.utbot.examples.samples.et;

public class ClassSimple {
    static public void doesNotThrow() {
        int y = 1;
    }

    static public void alwaysThrows() throws Exception {
        int y = 1;
        throw new Exception();
    }

    static public void maybeThrows(int x) throws IllegalArgumentException {
        int y = x;
        if (x < 0) {
            throw new IllegalArgumentException();
        }
    }
}

//
// #1. doesNotThrow
// #2. alwaysThrows
// #3. maybeThrows(-1) maybeThrows(0)
