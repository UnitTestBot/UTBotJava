package org.utbot.examples.assemble.constructors;

/**
 * A class with a primitive constructor that inherits another primitive constructor.
 */
public class InheritPrimitiveConstructor extends PrimitiveConstructor {

    private int c;
    private double d;

    public InheritPrimitiveConstructor(int a, int b, int c, double d) {
        super(b, a);
        this.c = c;
        this.d = d;
    }
}
