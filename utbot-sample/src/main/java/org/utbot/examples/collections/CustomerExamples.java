package org.utbot.examples.collections;

import java.util.HashMap;
import java.util.Map;

public class CustomerExamples {
    static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("key1", "value1");
        MAP.put("key2", "value2");
    }

    C c;

    public int simpleExample(String key) {
        if (MAP.containsKey(key)) {
            return 1;
        }
        return 2;
    }

    public int staticMap(A a, String key, int x) {
        B b = a.b;
        if (a.foo() > 1 && c.x < 3) {
            return 1;
        } else if (b.bar() < 3 && MAP.containsKey(key)) {
            return 2;
        } else if (c.x > 5 && foo(x) < 10) {
            return 3;
        } else {
            return 4;
        }
    }

    public int foo(int x) {
        return c.x + x;
    }
}

class C {
    int x;
    int y;

    public C(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class B {
    public int b;

    public int bar() {
        return 4 * b;
    }
}

class A {
    public B b;

    public int foo() {
        return b.b;
    }
}
