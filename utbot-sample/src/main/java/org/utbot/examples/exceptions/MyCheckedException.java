package org.utbot.examples.exceptions;

public class MyCheckedException extends Exception {
    private final int i;

    public MyCheckedException(int i) {
        this.i = i;
    }
}
