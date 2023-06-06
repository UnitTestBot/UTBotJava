package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintCleanerConditionsConfig.yaml
 */
public class TaintCleanerConditions {

    public String source() {
        return "t";
    }

    public String sourceEmpty() {
        return "";
    }

    public void sink(String s) {
        //
    }

    public void cleanerArgCondition(String s) {
        //
    }

    public boolean cleanerReturnCondition(String s) {
        return s.isEmpty();
    }

    public void badArg() {
        String s = source();
        cleanerArgCondition(s);
        sink(s);
    }

    public void badReturn() {
        String s = source();
        boolean isClean = cleanerReturnCondition(s);
        sink(s);
    }

    public void badThis() {
        String s = source();
        boolean res = s.isEmpty();
        sink(s);
    }

    public void goodArg() {
        String s = sourceEmpty();
        cleanerArgCondition(s);
        sink(s);
    }

    public void goodReturn() {
        String s = sourceEmpty();
        boolean isClean = cleanerReturnCondition(s);
        sink(s);
    }

    public void goodThis() {
        String s = sourceEmpty();
        boolean res = s.isEmpty();
        sink(s);
    }
}
