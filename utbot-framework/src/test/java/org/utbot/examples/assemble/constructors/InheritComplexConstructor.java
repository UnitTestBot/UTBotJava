package org.utbot.examples.assemble.constructors;

/**
 * A class with a primitive constructor that inherits a complex constructor.
 */
public class InheritComplexConstructor extends ComplexConstructor {
    private int c;
    private double d;

    public InheritComplexConstructor(int a, int b, int c, double d) {
        super(b, a);
        this.c = c;
        this.d = d;
    }
}
