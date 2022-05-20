package org.utbot.examples.objects;

public class SemiImmutableClass {
    public final int a;
    public int b;

    public SemiImmutableClass(int a, int b) {
        this.a = a;
        this.b = b;
    }
}
