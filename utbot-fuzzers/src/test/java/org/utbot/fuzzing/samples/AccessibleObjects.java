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

    public int ordinal(InnEn val) {
        switch (val) {
            case ONE:
                return 0;
            case TWO:
                return 1;
        }
        return -1;
    }

    private enum InnEn {
        ONE, TWO
    }
}
