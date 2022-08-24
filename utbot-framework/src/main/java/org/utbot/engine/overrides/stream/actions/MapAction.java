package org.utbot.engine.overrides.stream.actions;

import java.util.function.Function;

public class MapAction implements StreamAction {
    private final Function<Object, Object> mapping;

    public MapAction(Function<Object, Object> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        Object[] transformed = new Object[originArray.length];

        int i = 0;
        for (Object o : originArray) {
            transformed[i++] = mapping.apply(o);
        }

        return transformed;
    }
}
