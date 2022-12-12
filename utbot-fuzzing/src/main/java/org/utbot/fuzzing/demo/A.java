package org.utbot.fuzzing.demo;

/**
 * Example class that is used in {@link JavaFuzzingKt}
 */
@SuppressWarnings("unused")
final class A {

    public String name;
    public int age;
    public A copy;

    public A() {
    }

    public A(String name) {
        this.name = name;
    }

    public A(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "A{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", copy=" + copy +
                '}';
    }
}
