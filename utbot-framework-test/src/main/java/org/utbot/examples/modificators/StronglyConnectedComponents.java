package org.utbot.examples.modificators;

/**
 * A class with two strongly connected components in function calls.
 * https://habr.com/ru/post/331904/
 */
public class StronglyConnectedComponents {
    protected int x0, x1, x2, x3, x4;

    public void f0() {
        x0 = 1;
        f1();
    }

    public void f1() {
        x1 = 1;
        f2();
        f3();
    }

    private void f2() {
        f0();
        x2 = 1;
    }

    public void f3() {
        f4();
        x3 = 1;
    }

    public void f4() {
        x4 = 1;
        f3();
    }
}
