package org.utbot.examples.mock.service.impl;

public class ExampleClass {
    public int field;

    public ExampleClass(int a) {
        this.field = a;
    }

    static public void staticIncrementField(ExampleClass a) {
        a.field++;
    }

    public void incrementField() {
        field++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExampleClass that = (ExampleClass) o;
        return field == that.field;
    }

    @Override
    public int hashCode() {
        return field * 31;
    }
}
