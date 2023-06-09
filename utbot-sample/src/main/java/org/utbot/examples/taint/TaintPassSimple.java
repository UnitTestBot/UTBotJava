package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintPassSimpleConfig.yaml
 */
public class TaintPassSimple {

    public String source() {
        return "t";
    }

    public String pass(String s) {
        return s + "p";
    }

    public String notPass(String s) {
        return s + "p";
    }

    public void sink(String s) {
        //
    }

    public void bad() {
        String s = source();
        String sp = pass(s);
        sink(sp);
    }

    public void badDoublePass() {
        String s = source();
        String sp = pass(s);
        String spp = pass(sp);
        sink(spp);
    }

    public void good() {
        String s = source();
        String sp = notPass(s);
        sink(sp);
    }
}
