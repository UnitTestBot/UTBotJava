package org.utbot.engine.overrides.stream.actions;

import org.utbot.engine.overrides.UtArrayMock;

public class SkipAction implements StreamAction {
    private final int n;

    public SkipAction(int n) {
        this.n = n;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        final int curSize = originArray.length;

        if (n >= curSize) {
            return new Object[]{};
        }

        final int newSize = curSize - n;

        Object[] elements = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, elements, 0, newSize);

        return elements;
    }
}
