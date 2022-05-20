package org.utbot.examples.samples.et;

public class ClassSimpleNPE {
    public void A(boolean bad) {
        this.B();
        this.C();
        this.D(bad);
    }

    int b = 1;

    public void B() {
        while (b > 0) {
            b--;
            this.B();
        }
    }

    int c = 1;

    public void C() {
        while (c > 0) {
            c--;
            this.C();
        }
    }

    int d = 2;

    public void D(boolean bad) {
        ClassSimpleNPE kek = (bad ? null : this);
        while (d > 0) {
            d--;
            kek.D(bad);
        }
    }
}

// #1. A(false)
// #2. A(true)