package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintSimpleConfig.yaml
 */
public class TaintSimple {

    public String source() {
        return "t";
    }

    public void sink(String s) {
        //
    }

    public void bad() {
        String s = source();
        sink(s);
    }

    public void good() {
        String s = source();
        sink("n");
    }
}
