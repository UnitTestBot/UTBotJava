package org.utbot.examples.mock.fields;

public class ClassUsingClassWithRandomField {
    public int useClassWithRandomField() {
        ClassWithRandomField classWithRandomField = new ClassWithRandomField();

        return classWithRandomField.nextInt();
    }
}
