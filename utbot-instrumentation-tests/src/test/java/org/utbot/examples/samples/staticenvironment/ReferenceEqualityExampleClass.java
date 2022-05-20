package org.utbot.examples.samples.staticenvironment;

public class ReferenceEqualityExampleClass {
    public static InnerClass field1 = new InnerClass();
    public static InnerClass field2 = field1;
    public static InnerClass field3 = new InnerClass();

    public boolean test12() {
        return field1 == field2;
    }

    public boolean test23() {
        return field2 == field3;
    }

    public boolean test31() {
        return field3 == field1;
    }
}
