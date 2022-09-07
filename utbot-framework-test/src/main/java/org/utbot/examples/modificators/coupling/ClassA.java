package org.utbot.examples.modificators.coupling;

/**
 * A class with high coupling with [ClassB].
 */
public class ClassA {
    protected int v1, v2, v3, v4, v5, v6;

    public void a1Pub(ClassB objB) {
        a11Pr(1, objB);
        v1 = 1;
        a12Pr(objB);
    }

    public void a2Pub() {
        v2 = 1;
        a21Pr();
    }

    private void a11Pr(int a, ClassB objB) {
        if (a < 5) {
            a11Pr(a + 1, objB);
        } else {
            v3 = 1;
            System.out.println("Hello!");
        }
        a12Pr(objB);
    }

    private void a12Pr(ClassB objB) {
        v4 = 1;
        objB.b1Pub(this);
    }

    private void a21Pr() {
        v5 = 1;
        v6 = 1;
    }
}
