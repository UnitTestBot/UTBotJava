package org.utbot.engine.overrides.stream.actions;

import java.util.function.Function;

public class MapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final Function mapping;

    @SuppressWarnings("rawtypes")
    public MapAction(Function mapping) {
        this.mapping = mapping;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.apply(o);
        }

        return originArray;
    }
}
