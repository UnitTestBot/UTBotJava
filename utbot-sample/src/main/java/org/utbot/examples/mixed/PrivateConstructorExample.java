package org.utbot.examples.mixed;

/**
 * This class is used for testing the engine behavior
 * when working with classes that have only
 * private constructors
 */
public class PrivateConstructorExample {
    int a, b;

    private PrivateConstructorExample(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public int limitedSub(int limit) {
        int sub = a - b;
        if (sub >= limit) {
            return sub;
        }
        return limit;
    }
}
