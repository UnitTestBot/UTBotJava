package org.utbot.fuzzing.samples;

public class DeepNested {
    public class Nested1 {
        public class Nested2 {
            public int f(int i) {
                if (i > 0) {
                    return 10;
                }
                return 0;
            }
        }
    }
}
