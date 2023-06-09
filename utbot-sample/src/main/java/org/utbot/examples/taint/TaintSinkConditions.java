package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintSinkConditionsConfig.yaml
 */
public class TaintSinkConditions {

    public String source() {
        return "t";
    }

    public String sourceEmpty() {
        return "";
    }

    public void sink(String s, boolean isSink) {
        //
    }

    public void badArg() {
        String s = source();
        sink(s, true);
    }

    public void badThis() {
        String s = source();
        byte[] res = s.getBytes();
    }

    public void goodArg() {
        String s = source();
        sink(s, false);
    }

    public void goodThis() {
        String s = sourceEmpty();
        byte[] res = s.getBytes();
    }
}
