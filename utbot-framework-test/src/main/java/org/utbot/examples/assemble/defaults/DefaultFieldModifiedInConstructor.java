package org.utbot.examples.assemble.defaults;

public class DefaultFieldModifiedInConstructor {
    int z;

    @SuppressWarnings("Unused")
    DefaultFieldModifiedInConstructor(int z_) {
        z = z_;
        z = 10;
    }
}
