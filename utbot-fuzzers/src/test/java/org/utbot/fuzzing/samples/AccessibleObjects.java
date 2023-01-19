package org.utbot.fuzzing.samples;

@SuppressWarnings("unused")
public class AccessibleObjects {

    public boolean test(Inn.Node n) {
        return n.value * n.value == 36;
    }

    private static class Inn {
        static class Node {
            public int value;

            public Node() {

            }
        }
    }
}
