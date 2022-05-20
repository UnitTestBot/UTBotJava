package org.utbot.examples.objects;

public class SimpleClassExample {

    public int simpleCondition(SimpleDataClass c) {
        if (c.a < 5 && c.b > 10) {
            return 0;
        } else {
            c.a = 3;
            return c.a;
        }
    }

    public int singleFieldAccess(SimpleDataClass c) {
        int a = c.a;
        int b = c.b;
        if (a == 2 && b == 3) {
            return 0;
        } else if (a == 3 && b == 5) {
            return 1;
        } else {
            return 2;
        }
    }

    public int multipleFieldAccesses(SimpleDataClass c) {
        if (c.a == 2 && c.b == 3) {
            return 0;
        } else if (c.a == 3 && c.b == 5) {
            return 1;
        } else {
            return 2;
        }
    }

    public int immutableFieldAccess(SemiImmutableClass c) {
        if (c.b == 10) {
            return 0;
        } else {
            return 1;
        }
    }
}
