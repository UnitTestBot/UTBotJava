package org.utbot.examples.manual.examples.customer;

public class Demo9 {
    public B b0 = new B();
    public int test(B b1) {
        if (b0.c.integer == 0) {
            return 1;
        } else if (b1.getC().integer == 1) {
            return 2;
        }
        return 0;
    }
}
