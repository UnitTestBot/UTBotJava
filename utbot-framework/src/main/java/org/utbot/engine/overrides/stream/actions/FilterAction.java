package org.utbot.engine.overrides.stream.actions;

import org.utbot.engine.overrides.UtArrayMock;

import java.util.function.Predicate;

public class FilterAction implements StreamAction {
    private final Predicate<Object> filter;

    public FilterAction(Predicate<Object> filter) {
        this.filter = filter;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int newSize = 0;

        for (Object o : originArray) {
            if (filter.test(o)) {
                originArray[newSize++] = o;
            }
        }

        Object[] filtered = new Object[newSize];
        UtArrayMock.arraycopy(originArray, 0, filtered, 0, newSize);

        return filtered;
    }
}
