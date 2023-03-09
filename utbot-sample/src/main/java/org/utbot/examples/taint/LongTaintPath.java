package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/LongTaintPathConfig.yaml
 */
public class LongTaintPath {

    public String source() {
        return "tainted";
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
        sink("not tainted");
    }
}
