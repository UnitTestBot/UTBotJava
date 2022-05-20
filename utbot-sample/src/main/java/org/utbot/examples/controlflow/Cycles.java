package org.utbot.examples.controlflow;

public class Cycles {
    public int forCycle(int x) {
        for (int i = 0; i < x; i++) {
            if (i > 5) {
                return 1;
            }
        }
        return -1;
    }

    public int forCycleFour(int x) {
        for (int i = 0; i < x; i++) {
            if (i > 4) {
                return 1;
            }
        }
        return -1;
    }

    public int finiteCycle(int x) {
        while (true) {
            if (x % 519 == 0) {
                break;
            } else {
                x++;
            }
        }
        return x;
    }

    public int forCycleFromJayHorn(int x) {
        int r = 0;
        for (int i = 0; i < x; i++) {
            r += 2;
        }
        return r;
    }

    public int divideByZeroCheckWithCycles(int n, int x) {
        if (n < 5) {
            throw new IllegalArgumentException("n < 5");
        }
        int j = 0;
        for (int i = 0; i < n; i++) {
            j += i;
        }
        j /= x;
        for (int i = 0; i < n; i++) {
            j += i;
        }
        return 1;
    }

    public void moveToException(int x) {
        if (x < 400) {
            for (int i = x; i < 400; i++) {
                x++;
            }
        }

        if (x > 400) {
            for (int i = x; i > 400; i--) {
                x--;
            }
        }

        if (x == 400) {
            throw new IllegalArgumentException();
        }
    }

    public int whileCycle(int x) {
        int i = 0;
        int sum = 0;
        while (i < x) {
            sum += i;
            i += 1;
        }
        return sum;
    }

    public int callInnerWhile(int value) {
        return innerWhile(value, 42);
    }

    public int innerLoop(int value) {
        CycleDependedCondition cycleDependedCondition = new CycleDependedCondition();
        return cycleDependedCondition.twoCondition(value);
    }

    public int innerWhile(int a, int border) {
        int res = a;
        while (res >= border) {
            res = res - border;
        }
        return res;
    }

    public int loopInsideLoop(int x) {
        for (int i = x - 5; i < x; i++) {
            if (i < 0) {
                return 2;
            } else {
                for (int j = i; j < x + i; j++) {
                    if (j == 7) {
                        return 1;
                    }
                }
            }
        }
        return -1;
    }


    public int structureLoop(int x) {
        for (int i = 0; i < x; i++) {
            if (i == 2)
                return 1;
        }
        return -1;
    }
}
