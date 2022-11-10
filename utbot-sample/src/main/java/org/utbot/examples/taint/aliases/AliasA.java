package org.utbot.examples.taint.aliases;

public class AliasA {
    private String data;

    public AliasB f = new AliasB();
    public AliasB g = new AliasB();
    public AliasB h;

    public AliasA() {
    }

    public AliasA(AliasB b) {
        this.f = b;
    }

    public AliasB getF() {
        return f;
    }

    public AliasB getH() {
        return h;
    }

    public AliasB id(AliasB b) {
        return b;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}