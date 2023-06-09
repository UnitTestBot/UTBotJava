package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintSignatureConfig.yaml
 */
public class TaintSignature {

    // sources

    public String source() {
        return "t";
    }

    public String source(boolean cond) { // fake
        return "t";
    }

    public String source(int a, int b) { // fake
        return "t";
    }

    // passes

    public String pass(String s) {
        return s + "p";
    }

    public String pass(String s, int b) { // fake
        return s + "p";
    }

    // cleaners

    public void cleaner(String s) {
        //
    }

    public void cleaner(String s, String t) { // fake
        //
    }

    // sinks

    public void sink(String s) {
        //
    }

    public void sink(String s, int a) { // fake
        //
    }

    public void badFakeCleaner() {
        String s = source();
        String sp = pass(s);
        cleaner(sp, s); // fake
        sink(sp);
    }

    public void goodCleaner() {
        String s = source();
        String sp = pass(s);
        cleaner(sp);
        sink(sp);
    }

    public void goodFakeSources() {
        String s = source(true);
        String t = source(1, 2);
        sink(s);
        sink(t);
    }

    public void goodFakePass() {
        String s = source();
        String sp = pass(s, 1); // fake
        sink(sp);
    }

    public void goodFakeSink() {
        String s = source();
        sink(s, 1); // fake
    }
}
