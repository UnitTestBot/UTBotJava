package org.utbot.examples.objects;

public class HiddenFieldSuccClass extends HiddenFieldSuperClass {
    public double b;

    @Override
    public String toString() {
        return b + ". Super b: " + super.b;
    }
}