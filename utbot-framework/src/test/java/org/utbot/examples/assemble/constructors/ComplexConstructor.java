package org.utbot.examples.assemble.constructors;

/**
 * A class without default constructor and with complex one.
 *
 * Complex constructor contains statements differing from "this.a = a".
 */
public class ComplexConstructor {
    private int a, b;

    public ComplexConstructor(int a, int b) {
        this.a = a;
        this.b = a + b;
    }
}
