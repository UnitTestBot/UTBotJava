package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintLongPathConfig.yaml
 */
public class TaintLongPath {

    public String source() {
        return "t";
    }

    public void sink(String s) {
        //
    }

    public void bad() {
        String s = source();
        bad2(s);
    }

    public void bad2(String s) {
        bad3(s);
    }

    public void bad3(String s) {
        sink(s);
    }

    public void good() {
        String s = source();
        sink("n");
    }
}
