package org.utbot.examples.samples.et;

public class ClassSimpleRecursive {
    public static int A(int x) {
        if (x == 1) {
            return 42;
        }
        return A(x - 1);
    }

    public static int A_recursive(int x, int y) {
        if (x == 1) {
            if (y == 1) {
                throw new IllegalArgumentException();
            }
            return 42;
        }

        if (x == 2) {
            try {
                return A_recursive(x - 1, 1);
            } catch (IllegalArgumentException ignored) {
                String s = "catched!";
                throw new IndexOutOfBoundsException();
            }
        } else {
            if (y == 2) {
                try {
                    return A_recursive(x - 1, y);
                } catch (IndexOutOfBoundsException ignored) {
                    String s = "catched!";
                    return 42;
                }
            } else {
                return A_recursive(x - 1, y);
            }
        }
    }
}

// #1. A(3)
// #2. A_recursive(3, 2) A_recursive(3, 1)
