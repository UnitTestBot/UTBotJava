package org.utbot.engine.overrides.stream.actions;

import org.utbot.engine.overrides.UtArrayMock;

public class SkipAction implements StreamAction {
    private final long n;

    public SkipAction(long n) {
        this.n = n;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        final int curSize = originArray.length;

        if (n > curSize) {
            return new Object[]{};
        }

        // n is 1...Integer.MAX_VALUE here
        int newSize = (int) (curSize - n);

        if (newSize == 0) {
            return new Object[]{};
        }

        Object[] elements = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, elements, 0, newSize);

        return elements;
    }
}
