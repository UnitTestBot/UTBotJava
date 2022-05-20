package org.utbot.examples.codegen;

public class ClassWithVoidStaticMethods {
    static int x = 1;

    public static void changeStaticField(int x) {
        ClassWithVoidStaticMethods.x = x;
    }

    public static void throwException(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("Less than zero value");
        }

        if (x > 0) {
            throw new IllegalStateException("More than zero value");
        }

        changeStaticField(x);
    }
}
