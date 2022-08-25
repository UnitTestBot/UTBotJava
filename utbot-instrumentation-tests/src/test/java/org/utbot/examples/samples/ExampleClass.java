package org.utbot.examples.samples;

@SuppressWarnings("All")
public class ExampleClass {
    int x1 = 1;

    boolean[] arr = new boolean[5];

    boolean[] arr2 = new boolean[10];

    public void bar(int x) {
        if (x > 1) {
            x1++;
            x1++;
        } else {
            x1--;
            x1--;
        }
    }

    public void kek2(int x) {
        arr[x] = true;
    }

    public int foo(int x) {
        x1 = x ^ 2;

        boolean was = false;

        for (int i = 0; i < x; i++) {
            was = true;
            int x2 = 0;
            if (i > 5) {
                was = false;
                x2 = 1;
            }
            if (was && x2 == 0) {
                was = true;
            }
        }

        // empty lines
        return was ? x1 : x1 + 1;
    }

    public void dependsOnField() {
        x1 = x1 ^ 1;
        if ((x1 & 1) == 1) {
            x1 += 4;
        } else {
            x1 += 2;
        }
    }

    public int dependsOnFieldReturn() {
        x1 = x1 ^ 1;
        if ((x1 & 1) == 1) {
            x1 += 4;
        } else {
            x1 += 2;
        }
        return x1;
    }

    public void emptyMethod() {
    }

    @SuppressWarnings("unused")
    public int use() {
        return arr2.length;
    }
}