package org.utbot.examples.structures;

public class MinStackExample {

    // TODO JIRA:406
    // bug with default values. We have to initialize them in constructor, but we don't
    // So we have null array inside and other strange values
    // For now I'll set field to the right values by hand
    public MinStack create(long[] values) {
        if (values.length < 3) {
            throw new IllegalArgumentException();
        }
        values[0] += 100;
        values[1] += 200;
        values[2] -= 300;

        MinStack stack = construct(new long[0]);

        for (long value : values) {
            stack.addValue(value);
        }

        long value = stack.getMin();
        stack.addValue(value - 3000);

        return stack;
    }

    public MinStack addSingleValue(long[] initialValues) {
        MinStack stack = construct(initialValues);

        long minValue = stack.getMin();
        long value = minValue - 100;
        stack.addValue(value);
        return stack;
    }

    public long getMinValue(long[] initialValues) {
        MinStack stack = construct(initialValues);

        stack.addValue(-1);
        stack.addValue(4);
        stack.addValue(-500);
        stack.removeValue();
        return stack.getMin();
    }

    public MinStack removeValue(long[] initialValues) {
        MinStack stack = construct(initialValues);

        stack.removeValue();
        return stack;
    }

    public MinStack construct(long[] values) {
        MinStack stack = new MinStack();
        stack.size = 0;
        stack.minStack = new long[10];
        stack.stack = new long[10];

        for (long value : values) {
            stack.addValue(value);
        }

        return stack;
    }
}
