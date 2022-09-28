package org.utbot.examples.assemble.defaults;

public class DefaultFieldModifiedInConstructor {
    public int z;

    @SuppressWarnings("Unused")
    DefaultFieldModifiedInConstructor(int z_) {
        z = z_;
        z = 10;
    }
}
