package org.utbot.examples.structures;

public class MinStack {
    long[] stack;
    long[] minStack;
    int size;

    public MinStack() {
        stack = new long[10];
        minStack = new long[10];
        size = 0;
    }

    public void addValue(long value) {
        stack[size] = value;
        if (size == 0) {
            minStack[size] = value;
        } else {
            minStack[size] = Math.min(minStack[size - 1], value);
        }
        size++;
    }

    public void removeValue() {
        if (size <= 0) {
            throw new RuntimeException("Stack has no elements");
        }
        size--;
    }

    public long getMin() {
        return minStack[size - 1];
    }
}
