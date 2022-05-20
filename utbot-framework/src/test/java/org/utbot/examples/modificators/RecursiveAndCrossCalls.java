package org.utbot.examples.modificators;

/**
 * A class with recursive call and cross calls that modify fields.
 */
public class RecursiveAndCrossCalls {
    protected int x, y, z, t;

    public void setRecursively(int a) {
        if (a < 5) {
            setRecursively(a + 1);
        } else {
            x = 1;
            setPrivate();
        }
    }

    public void setWithReverseCalls() {
        setPrivateWithReverseCall(0);
    }

    private void setPrivateWithReverseCall(int a) {
        if (a < 10) {
            setPrivateForReverseCall(a + 1);
        } else {
            t = 1;
        }
    }

    private void setPrivateForReverseCall(int a) {
        setPrivateWithReverseCall(a);
        x = 1;
        setPrivate();
    }

    private void setPrivate() {
        z = 1;
        setPrivateFromPrivate();
    }

    private void setPrivateFromPrivate() {
        y = 1;
    }
}
