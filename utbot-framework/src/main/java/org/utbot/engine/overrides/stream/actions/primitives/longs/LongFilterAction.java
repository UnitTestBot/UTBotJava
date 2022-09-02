package org.utbot.engine.overrides.stream.actions.primitives.longs;

import org.utbot.engine.overrides.UtArrayMock;
import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.LongPredicate;

public class LongFilterAction implements StreamAction {
    private final LongPredicate filter;

    public LongFilterAction(LongPredicate filter) {
        this.filter = filter;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int newSize = 0;

        for (Object o : originArray) {
            if (filter.test((Long) o)) {
                originArray[newSize++] = o;
            }
        }

        Object[] filtered = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, filtered, 0, newSize);

        return filtered;
    }
}
