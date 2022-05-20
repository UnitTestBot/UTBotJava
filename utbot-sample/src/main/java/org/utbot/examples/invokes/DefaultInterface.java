package org.utbot.examples.invokes;

// interface with a default implementation of foo
public interface DefaultInterface {
    default int foo() {
        throw new UnsupportedOperationException();
    }
}

// class with an implementation of foo
class BaseClass {
    public int foo() {
        return 0;
    }
}

// Derived class does not provide implementation of foo
class DerivedClass extends BaseClass implements DefaultInterface {
}
