package org.utbot.examples.reflection;

public class ClassWithDifferentModifiers {
    @SuppressWarnings({"All"})
    private int privateField;

    private static final Wrapper privateStaticFinalField = new Wrapper(1);

    public ClassWithDifferentModifiers() {
        privateField = 0;
    }

    int packagePrivateMethod() {
        return 1;
    }

    private int privateMethod() {
        return 1;
    }

    public static class Wrapper {
        public int x;
        public Wrapper(int x) {
            this.x = x;
        }
    }
}
