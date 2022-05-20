package org.utbot.examples.samples.jacoco;

public class ExceptionExamples {
    public int initAnArray(int n) {
        try {
            int[] a = new int[n];
            a[n - 1] = n + 1;
            a[n - 2] = n + 2;
            return a[n - 1] + a[n - 2];
        } catch (NullPointerException e) {
            return -1; // Unreachable branch
        } catch (NegativeArraySizeException e) {
            return -2;
        } catch (IndexOutOfBoundsException e) {
            return -3;
        }
    }

    public int nestedExceptions(int i) {
        try {
            return checkAll(i);
        } catch (NullPointerException e) {
            return 100;
        } catch (RuntimeException e) {
            return -100;
        }
    }

    public int doNotCatchNested(int i) {
        return checkAll(i);
    }

    private int checkAll(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Negative");
        }
        return checkPositive(i);
    }

    private int checkPositive(int i) {
        if (i > 0) {
            throw new NullPointerException("Positive");
        }
        return 0;
    }

    @SuppressWarnings({"CaughtExceptionImmediatelyRethrown", "finally", "ThrowFromFinallyBlock"})
    public int finallyThrowing(int i) {
        try {
            return checkPositive(i);
        } catch (NullPointerException e) {
            throw e;
        } finally {
            throw new IllegalStateException("finally");
        }
    }

    public int finallyChanging(int i) {
        int r = i * 2;
        try {
            checkPositive(r);
        } catch (NullPointerException e) {
            r += 100;
        } finally {
            r += 10;
        }
        return r;
    }

    public int throwException(int i) {
        int r = 1;
        if (i > 0) {
            r += 10;
            System.mapLibraryName(null);
        } else {
            r += 100;
        }
        return r;
    }

    public int catchDeepNestedThrow(int i) {
        try {
            return callNestedWithThrow(i);
        } catch (Exception e) {
            throw new NullPointerException();
        }
    }

    public IllegalArgumentException createException() {
        return new IllegalArgumentException("Here we are: " + Math.sqrt(10));
    }

    public int dontCatchDeepNestedThrow(int i) {
        return callNestedWithThrow(i);
    }

    private int callNestedWithThrow(int i) {
        return nestedWithThrow(i);
    }

    private int nestedWithThrow(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Negative");
        }
        return i;
    }
}