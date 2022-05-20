package org.utbot.examples.codegen;

public class AnotherClassWithVoidStaticMethods {
    public static void throwException(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("Less than zero value");
        }
    }
}
