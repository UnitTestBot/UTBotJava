package org.utbot.examples.assemble;

/**
 * A class with private fields of primitive type
 * having default constructor, setters and direct accessors for them.
 */
public class PrimitiveFields {
    private int a;
    protected int b;

    public PrimitiveFields() {
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }
}
