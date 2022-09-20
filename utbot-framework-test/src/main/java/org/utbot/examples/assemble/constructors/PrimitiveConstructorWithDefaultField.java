package org.utbot.examples.assemble.constructors;

/**
 * A class without default constructor and with another one with default field
 */
public class PrimitiveConstructorWithDefaultField {
    private int a;
    private int b = 5;

    public PrimitiveConstructorWithDefaultField(int a) {
        this.a = a;
    }
}
