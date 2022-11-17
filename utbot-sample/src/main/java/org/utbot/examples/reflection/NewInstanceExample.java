package org.utbot.examples.reflection;

public class NewInstanceExample {
    @SuppressWarnings("deprecation")
    int createWithReflectionExample() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> cls = Class.forName("org.utbot.examples.reflection.ClassWithDefaultConstructor");
        ClassWithDefaultConstructor classWithDefaultConstructor = (ClassWithDefaultConstructor) cls.newInstance();

        return classWithDefaultConstructor.x;
    }

    /*@SuppressWarnings("deprecation")
    int createWithReflectionWithExceptionExample() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> cls = Class.forName("org.utbot.examples.reflection.ClassWithoutDefaultConstructor");
        ClassWithoutDefaultConstructor classWithoutDefaultConstructor = (ClassWithoutDefaultConstructor) cls.newInstance(); // An exception will be thrown here

        return classWithoutDefaultConstructor.x;
    }*/
}

class ClassWithDefaultConstructor {

    int x;
}

/*class ClassWithoutDefaultConstructor {
    int x;

    public ClassWithoutDefaultConstructor(int x) {
        this.x = x;
    }
}*/
