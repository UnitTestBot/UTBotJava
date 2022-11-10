package org.utbot.examples.taint.alias;

public class AliasG implements AliasI {
    AliasA a;

    @Override
    public AliasA foo(AliasA a) {
        this.a = a;

        return a;
    }
}
