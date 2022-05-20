package org.utbot.examples.objects;

public class PrivateFields {
    public boolean accessWithGetter(ClassWithPrivateField foo) {
        return foo.getA() == 1;
    }
}
