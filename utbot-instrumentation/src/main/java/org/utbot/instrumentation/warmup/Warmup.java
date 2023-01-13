package org.utbot.instrumentation.warmup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Warmup {
    public int x = 0;

    public Warmup(int x) {
        this.x = x;
    }

    public class Nested {
        private String kek = "kek";
        public int t = 1;
        Nested(int y) {
            t = x + y;
        }
    }

    public static class StaticNested {
        private String lol = "lol";

    }

    public Nested doWarmup1(Warmup warmup) {
        return new Nested(x);
    }

    public int doWarmup2(int []array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        if (sum % 2 == 0) {
            throw new RuntimeException();
        } else {
            return sum;
        }
    }

    static int STATIC_X = 10;
    private String lol = "1234";
    List<Integer> list = new ArrayList<>();
    HashMap<Integer, Set<String>> hashMap = new HashMap<>();
}
