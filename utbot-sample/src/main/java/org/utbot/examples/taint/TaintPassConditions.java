package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintPassConditionsConfig.yaml
 */
public class TaintPassConditions {

    public String source() {
        return "t";
    }

    public String sourceEmpty() {
        return "";
    }

    public void sink(String s) {
        //
    }

    public String passArgCondition(String s, boolean isPass) {
        if (isPass) {
            return s + "p";
        } else {
            return "";
        }
    }

    public String passReturnCondition(String s, boolean isPass) {
        if (isPass) {
            return s + "p";
        } else {
            return "";
        }
    }

    public void badArg() {
        String s = source();
        String sp = passArgCondition(s, true);
        sink(sp);
    }

    public void badReturn() {
        String s = source();
        String sp = passReturnCondition(s, true);
        sink(sp);
    }

    public void badThis() {
        String s = source();
        String sp = s.concat("#");
        sink(sp);
    }

    public void goodArg() {
        String s = source();
        String sp = passArgCondition(s, false);
        sink(sp);
    }

    public void goodReturn() {
        String s = source();
        String sp = passReturnCondition(s, false);
        sink(sp);
    }

    public void goodThis() {
        String s = sourceEmpty();
        String sp = s.concat("#");
        sink(sp);
    }
}
