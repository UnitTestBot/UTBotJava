package org.utbot.examples.codegen;

public class ClassFromTheSamePackage {
    public final static int a = 1;
    public static int b = 2;

    static int foo(Integer a) {
        if (a == null) {
            return -1;
        }

        if (a == Integer.MAX_VALUE) {
            return 1;
        }

        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
