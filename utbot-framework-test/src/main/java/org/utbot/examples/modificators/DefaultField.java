package org.utbot.examples.modificators;

/**
 * A class with a field with default value that is not a default value of type.
 */
public class DefaultField {
    int z = 10;
    int x;

    public int foo() {
        z++;
        return z;
    }

}
