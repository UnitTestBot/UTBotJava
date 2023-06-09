package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintSourceConditionsConfig.yaml
 */
public class TaintSourceConditions {

    public String sourceArgCondition(boolean isSource) {
        if (isSource) {
            return "t";
        } else {
            return "";
        }
    }

    public String sourceReturnCondition(boolean isSource) {
        if (isSource) {
            return "t";
        } else {
            return "";
        }
    }

    public void sink(String s) {
        //
    }

    public void badArg() {
        String s = sourceArgCondition(true);
        sink(s);
    }

    public void badReturn() {
        String s = sourceReturnCondition(true);
        sink(s);
    }

    public void badThis() {
        String s = "t";
        String res = s.toLowerCase();
        sink(res);
    }

    public void goodArg() {
        String s = sourceArgCondition(false);
        sink(s);
    }

    public void goodReturn() {
        String s = sourceArgCondition(false);
        sink(s);
    }

    public void goodThis() {
        String s = "";
        String res = s.toLowerCase();
        sink(res);
    }
}
