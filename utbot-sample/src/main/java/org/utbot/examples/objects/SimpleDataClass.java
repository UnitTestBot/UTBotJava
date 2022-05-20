package org.utbot.examples.objects;

public class SimpleDataClass {
    public int a;
    public int b;

    public static SimpleDataClass staticField;

    public SimpleDataClass() {
    }

    public SimpleDataClass(int a, int b) {
        this.a = a;
        this.b = b;
    }
}