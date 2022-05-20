package org.utbot.examples.samples;

public class ClassWithInnerClasses {
    int x;

    public ClassWithInnerClasses(int x) {
        this.x = x;
    }

    public class InnerClass {
        public int sum(int a, int b) {
            if (a + b == 4) {
                return -1;
            }
            return x + a + b + InnerStaticClass.mul(a, b);
        }
    }

    public static class InnerStaticClass {
        public static int mul(int a, int b) {
            return a * b;
        }
    }

    public int doSomething(int a, int b) {
        InnerClass innerClass = new InnerClass();
        return innerClass.sum(a, b);
    }
}