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

class GreatCompound {
    Compound compound;

    GreatCompound(Compound compound) {
        this.compound = compound;
    }
}

public class ClassWithNullableField {
    public Compound returnCompoundWithNullableField(int value) {
        if (value > 0) return new Compound(null);
        else return new Compound(new Component());
    }

    public GreatCompound returnGreatCompoundWithNullableField(int value) {
        if (value > 0) return new GreatCompound(null);
        else if (value == 0) return new GreatCompound(new Compound(new Component()));
        else return new GreatCompound(new Compound(null));
    }
}
