package org.utbot.examples.controlflow;

public class Conditions {
    public int simpleCondition(boolean condition) {
        if (condition) {
            return 1;
        } else {
            return 0;
        }
    }

    public void emptyBranches(boolean condition) {
        if (condition) {
            // do nothing
        } else {
            // do nothing
        }
    }
}
