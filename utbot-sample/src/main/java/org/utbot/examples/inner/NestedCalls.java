package org.utbot.examples.inner;

public class NestedCalls {
    public int callInitExamples(int n) {
        ExceptionExamples exceptionExamples = new ExceptionExamples();
        return exceptionExamples.initAnArray(n);
    }

    class ExceptionExamples {
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
    }
}
