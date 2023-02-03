package org.utbot.examples.arrays;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;

public class CopyOfExample {
    @SuppressWarnings("IfStatementWithIdenticalBranches")
    Integer[] copyOfExample(Integer[] values, int newLength) {
        UtMock.assume(values != null);

        if (values.length == 0) {
            if (newLength <= 0) {
                return Arrays.copyOf(values, newLength);
            } else {
                return Arrays.copyOf(values, newLength);
            }
        } else {
            if (newLength <= 0) {
                return Arrays.copyOf(values, newLength);
            } else {
                return Arrays.copyOf(values, newLength);
            }
        }
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    Integer[] copyOfRangeExample(Integer[] values, int from, int to) {
        UtMock.assume(values != null);

        if (from < 0) {
            return Arrays.copyOfRange(values, from, to);
        }

        if (from > to) {
            return Arrays.copyOfRange(values, from, to);
        }

        if (from > values.length) {
            return Arrays.copyOfRange(values, from, to);
        }

        return Arrays.copyOfRange(values, from, to);
    }
}
