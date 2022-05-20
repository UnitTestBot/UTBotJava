package org.utbot.examples.codegen;

public class VoidStaticMethodsTestingClass {
    public void invokeChangeStaticFieldMethod(int x) {
        ClassWithVoidStaticMethods.changeStaticField(x);
        if (ClassWithVoidStaticMethods.x == 10) {
            throw new RuntimeException("Value equals 10");
        }
    }

    public void invokeThrowExceptionMethod(int x) {
        ClassWithVoidStaticMethods.throwException(x);
    }

    public void invokeAnotherThrowExceptionMethod(int x) {
        AnotherClassWithVoidStaticMethods.throwException(x);
    }
}
