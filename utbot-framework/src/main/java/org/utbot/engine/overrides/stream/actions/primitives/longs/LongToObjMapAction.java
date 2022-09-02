package org.utbot.engine.overrides.stream.actions.primitives.longs;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.LongFunction;

public class LongToObjMapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final LongFunction mapping;

    @SuppressWarnings("rawtypes")
    public LongToObjMapAction(LongFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.apply((Long) o);
        }

        return originArray;
    }
}
