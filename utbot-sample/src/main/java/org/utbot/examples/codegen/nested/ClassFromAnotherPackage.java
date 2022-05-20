package org.utbot.examples.codegen.nested;

import java.util.Objects;

public class ClassFromAnotherPackage {
    public final static int a = 1;
    public static int b = 2;
    private final int d;
    public int c;

    public ClassFromAnotherPackage(int c) {
        this.c = c;
        d = 5;
    }

    public ClassFromAnotherPackage(int c, int d) {
        this.c = c;
        this.d = d;
    }

    public static int constValue() {
        return 42;
    }

    public int getD() {
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassFromAnotherPackage that = (ClassFromAnotherPackage) o;
        return d == that.d && c == that.c;
    }

    @Override
    public int hashCode() {
        return Objects.hash(d, c);
    }
}
