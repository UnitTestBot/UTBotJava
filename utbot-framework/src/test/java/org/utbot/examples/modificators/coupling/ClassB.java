package org.utbot.examples.modificators.coupling;

/**
 * A class with high coupling with [ClassA].
 */
public class ClassB {
    protected int w1, w2, w3, w4;

    public void b1Pub(ClassA objA) {
        w1 = 1;
        objA.v6 += 1;
        b11Pr();
        b12Pr();

        if (objA.v6 == 0) {
            b1Pub(objA);
        }
    }

    public void b2Pub() {
        w2 = 1;
    }

    private void b11Pr() {
        w3 = 1;
    }

    private void b12Pr() {
        w4 = 1;
    }
}
