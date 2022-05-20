package org.utbot.examples.assemble.statics;

/**
 * A class with primitive constructor and static field
 */
public class StaticField {
    public int a;
    public int b;

    public static StaticField staticField;

    public StaticField() {
    }

    public StaticField(int a, int b) {
        this.a = a;
        this.b = b;
    }
}