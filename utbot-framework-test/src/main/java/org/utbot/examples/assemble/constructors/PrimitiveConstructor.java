package org.utbot.examples.assemble.constructors;

/**
 * A class without default constructor and with primitive one.
 *
 * Primitive constructor contains only statements like "this.a = a".
 */
public class PrimitiveConstructor {
    private int a, b;

    public PrimitiveConstructor(int a, int b) {
        this.a = a;
        this.b = b;
    }
}
