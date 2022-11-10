package org.utbot.examples.taint.aliases;

public class AliasQ {
    private AliasA a;

    public AliasQ(AliasA a) {
        this.a = a;
    }

    public void alias(AliasA x) {
        this.a = x;
    }

    public AliasA getA() {
        return a;
    }
}
