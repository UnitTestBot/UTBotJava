package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintCleanerSimpleConfig.yaml
 */
public class TaintCleanerSimple {

    public String source() {
        return "t";
    }

    public void cleaner(String s) {
        //
    }

    public void notCleaner(String s) {
        //
    }

    public void sink(String s) {
        //
    }

    public void bad() {
        String s = source();
        notCleaner(s);
        sink(s);
    }

    public void good() {
        String s = source();
        cleaner(s);
        sink(s);
    }
}
