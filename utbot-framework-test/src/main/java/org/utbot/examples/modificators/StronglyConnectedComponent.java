package org.utbot.examples.modificators;

/**
 * A class with one strongly connected component in function calls.
 */
public class StronglyConnectedComponent {
    protected int x0, x1, x2, x3, x4;

    public void f0() {
        x0 = 1;
        f1();
    }

    public void f1() {
        x1 = 1;
        f2();
    }

    private void f2() {
        f3();
        x2 = 1;
    }

    private void f3() {
        f1();
        x3 = 1;
        f4();
    }

    public void f4() {
        x4 = 1;
    }
}

