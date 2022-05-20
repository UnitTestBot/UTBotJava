package org.utbot.instrumentation.samples.mock;

@SuppressWarnings("unused")
public class ClassForMockConstructor {
    int x = 10;
    String s = "123";

    public ClassForMockConstructor() {
        x = 15;
    }

    public ClassForMockConstructor(String s) {
        this.s = s;
    }

    public Object getNewInstance(String s) {
        Object instance;
        if (s == null) {
            instance = new ClassForMockConstructor();
        } else if (s.equals("")) {
            instance = new Object();
        } else {
            instance = new ClassForMockConstructor(s);
        }
        return instance;
    }

    public static class InnerClass {

        String s;

        public InnerClass(String s) {
            this.s = s;
        }

        public ClassForMockConstructor getNewInstance() {
            if (s == null) {
                return new ClassForMockConstructor();
            } else {
                return new ClassForMockConstructor(s);
            }
        }
    }

}
