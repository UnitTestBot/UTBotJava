package org.utbot.examples.taint.alias;

/*
 * @description 将source转换成char数组，传递给SQL API。
 *
 * @bad bad
 * @cwe 89
 * @tool fortify: SQL Injection;secbrella: SecS_SQL_Injection
 * @author 方健尔 f00563108
 */
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