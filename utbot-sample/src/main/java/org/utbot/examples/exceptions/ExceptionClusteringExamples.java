package org.utbot.examples.exceptions;

public class ExceptionClusteringExamples {
    @SuppressWarnings("divzero")
    public int differentExceptions(int i) throws MyCheckedException {
        if (i == 0) { // unchecked implicit exception throwing
            return 100 / i;
        }
        if (i == 1) { // checked explicit exception throwing
            throw new MyCheckedException(i);
        }
        if (i == 2) { // unchecked explicit exception throwing
            throw new IllegalArgumentException("This is two!");
        }
        return i * 2;
    }

    public int differentExceptionsInNestedCall(int i) throws MyCheckedException {
        return differentExceptions(i);
    }

    public int sleepingMoreThanDefaultTimeout(int i) throws InterruptedException {
        Thread.sleep(1500L);

        if (i < 0) {
            throw new RuntimeException();
        }

        return i;
    }
}
