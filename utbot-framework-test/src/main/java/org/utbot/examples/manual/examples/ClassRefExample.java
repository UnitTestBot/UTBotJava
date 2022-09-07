package org.utbot.examples.manual.examples;

public class ClassRefExample {
    private final Class<String> stringClass = String.class;

    public boolean assertInstance(Class<?> clazz) {
        return stringClass.getCanonicalName().equals(clazz.getCanonicalName());
    }
}
