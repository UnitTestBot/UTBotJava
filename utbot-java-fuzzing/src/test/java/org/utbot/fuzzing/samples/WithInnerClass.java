package org.utbot.fuzzing.samples;

public class WithInnerClass {
    public class NonStatic {
        public int x;
        public NonStatic(int x) { this.x = x; }
    }
    int f(NonStatic b) {
        return b.x * b.x;
    }

    public static class Static {
        public int x;
        public Static(int x) { this.x = x; }
    }
    int g(Static b) {
        return b.x * b.x;
    }
}
