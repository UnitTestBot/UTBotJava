package org.utbot.examples.assemble.constructors;

/**
 * A class without default constructor and with complex one,
 * having a setter for field with complex assignment.
 *
 * Complex assignment is different from "this.a = a".
 */
public class ComplexConstructorWithSetter {
    private int a, b;

    public ComplexConstructorWithSetter(int a, int b) {
        this.a = a;
        this.b = a + b;
    }

    public void setB(int b) { this.b = b; }
}
