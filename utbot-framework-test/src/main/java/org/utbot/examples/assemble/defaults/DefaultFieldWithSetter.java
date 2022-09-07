package org.utbot.examples.assemble.defaults;

/**
 * A class with a field with setter default value that is not a default value of type.
 */
public class DefaultFieldWithSetter {
    private int z = 10;


    public void setZ(int z) {
        this.z = z;
    }
}
