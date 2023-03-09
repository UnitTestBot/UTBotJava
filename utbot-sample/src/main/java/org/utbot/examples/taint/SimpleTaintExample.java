package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/SimpleTaintExampleConfig.yaml
 */
public class SimpleTaintExample {

    public String source() {
        return "tainted";
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
        sink("not tainted");
    }
}
