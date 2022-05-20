package org.utbot.examples.modificators;

/**
 * A class with fields modifying method that is invoked in assignment.
 */
public class InvokeInAssignment {
    protected int x, y;

    public void fun(int number) {
        Boolean isPos = isPositive(number);
        if (isPos) {
            x = 1;
        } else {
            x = 2;
        }
    }

    private Boolean isPositive(int number) {
        y = number;
        return number > 0;
    }
}

