package org.utbot.examples.codegen.deepequals;

class Component {
    int a = 1;
}

class Compound {
    Component component;

    Compound(Component component) {
        this.component = component;
    }
}

public class ClassWithNullableField {
    public Compound returnCompoundWithNullableField(int value) {
        if (value > 0) return new Compound(null);
        else return new Compound(new Component());
    }
}
