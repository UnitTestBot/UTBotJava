package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintBranchingConfig.yaml
 */
public class TaintBranching {

    public String source() {
        return "t";
    }

    public void sink(String s) {
        //
    }

    public void bad(boolean cond) {
        String s = source();
        if (cond) {
            sink(s);
        } else {
            //
        }
    }

    public void good(boolean cond) {
        String s = source();
        if (cond) {
            sink("n");
        } else {
            //
        }
    }
}
