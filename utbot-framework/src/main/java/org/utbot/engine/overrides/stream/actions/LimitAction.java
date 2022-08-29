package org.utbot.engine.overrides.stream.actions;

import org.utbot.engine.overrides.UtArrayMock;

public class LimitAction implements StreamAction {
    private final int maxSize;

    public LimitAction(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        if (maxSize == 0) {
            return new Object[]{};
        }

        final int curSize = originArray.length;
        int newSize = maxSize;

        if (newSize > curSize) {
            newSize = curSize;
        }

        Object[] elements = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, elements, 0, newSize);

        return elements;
    }
}
