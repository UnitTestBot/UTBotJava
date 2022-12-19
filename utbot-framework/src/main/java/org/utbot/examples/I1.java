package org.utbot.examples;

public interface I1<R> extends I2<R> {
    int lol2();
    default int lol3() {
        return 3;
    }

    void lol4();
}
