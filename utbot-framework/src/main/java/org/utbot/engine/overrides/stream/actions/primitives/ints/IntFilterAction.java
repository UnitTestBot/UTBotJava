package org.utbot.engine.overrides.stream.actions.primitives.ints;

import org.utbot.engine.overrides.UtArrayMock;
import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.IntPredicate;

public class IntFilterAction implements StreamAction {
    private final IntPredicate filter;

    public IntFilterAction(IntPredicate filter) {
        this.filter = filter;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int newSize = 0;

        for (Object o : originArray) {
            if (filter.test((Integer) o)) {
                originArray[newSize++] = o;
            }
        }

        Object[] filtered = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, filtered, 0, newSize);

        return filtered;
    }
}
