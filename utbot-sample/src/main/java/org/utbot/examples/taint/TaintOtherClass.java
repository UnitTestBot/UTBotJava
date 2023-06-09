package org.utbot.examples.taint;

public class TaintOtherClass {

    public void bad() {
        Inner inner = new Inner();
        String s = inner.source();
        String sp = inner.pass(s);
        inner.sink(sp);
    }

    public void good() {
        Inner inner = new Inner();
        String s = inner.source();
        inner.cleaner(s);
        inner.sink(s);
    }
}

class Inner {

    public String source() {
        return "t";
    }

    public String pass(String s) {
        return s + "p";
    }

    public void cleaner(String s) {
        //
    }

    public void sink(String s) {
        //
    }
}
