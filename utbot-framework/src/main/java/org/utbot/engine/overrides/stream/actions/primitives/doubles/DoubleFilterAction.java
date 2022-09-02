package org.utbot.engine.overrides.stream.actions.primitives.doubles;

import org.utbot.engine.overrides.UtArrayMock;
import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.DoublePredicate;

public class DoubleFilterAction implements StreamAction {
    private final DoublePredicate filter;

    public DoubleFilterAction(DoublePredicate filter) {
        this.filter = filter;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int newSize = 0;

        for (Object o : originArray) {
            if (filter.test((Double) o)) {
                originArray[newSize++] = o;
            }
        }

        Object[] filtered = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, filtered, 0, newSize);

        return filtered;
    }
}
