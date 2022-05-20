package org.utbot.examples.assemble;

/**
 * A class with private field of complex type
 * having default constructor and all appropriate setters.
 */
public class ComplexField {
    private int i;
    private PrimitiveFields s;

    public ComplexField() {
    }

    public void setI(int i) { this.i = i; }

    public void setS(PrimitiveFields s) {
        this.s = s;
    }
}
