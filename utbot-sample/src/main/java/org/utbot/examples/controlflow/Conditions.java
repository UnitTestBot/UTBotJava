package org.utbot.examples.controlflow;

public class Conditions {

    /**
     * This Doc is here in order to check whether the summaries and display names are rendered correctly.
     * Had not in hours of peace,
     * It learned to lightly look on life.
     *
     * @param a some long value
     * @param b some int value
     * @return the result you won't expect.
     */
    public int returnCastFromTernaryOperator(long a, int b) {
        a = a % b;
        return (int) (a < 0 ? a + b : a);
    }

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
