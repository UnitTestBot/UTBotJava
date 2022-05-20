package org.utbot.examples.mixed;

public class Overload {
    public int sign(int x) {
        if (x > 0) {
            return 1;
        } else if (x < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    public int sign(int x, int y) {
        if (x + y > 0) {
            return 1;
        } else if (x + y < 0) {
            return -1;
        } else {
            return 0;
        }
    }
}