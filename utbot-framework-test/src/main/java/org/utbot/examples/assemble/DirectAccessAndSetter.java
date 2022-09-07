package org.utbot.examples.assemble;

/**
 * A class with public fields allowing direct access and having setter also.
 */
public class DirectAccessAndSetter {
    public int a;
    public PrimitiveFields p;

    public void setA(int a) {
        this.a = a;
    }

    public void setP(PrimitiveFields p) {
        this.p = p;
    }
}
