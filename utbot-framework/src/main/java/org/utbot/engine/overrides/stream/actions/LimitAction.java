package org.utbot.engine.overrides.stream.actions;

import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;

public class LimitAction implements StreamAction {
    private final long maxSize;

    public LimitAction(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        if (maxSize == 0) {
            return new Object[]{};
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        final int curSize = originArray.length;
        int newSize = (int) maxSize;

        if (newSize > curSize) {
            newSize = curSize;
        }

        Object[] elements = new Object[newSize];
        int i = 0;

        for (Object element : originArray) {
            if (i >= newSize) {
                break;
            }

            elements[i++] = element;
        }

        return elements;
    }
}
