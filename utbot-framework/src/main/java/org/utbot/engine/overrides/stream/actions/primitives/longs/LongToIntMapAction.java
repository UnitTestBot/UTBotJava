package org.utbot.engine.overrides.stream.actions.primitives.longs;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;

public class LongToIntMapAction implements StreamAction {
    private final LongToIntFunction mapping;

    public LongToIntMapAction(LongToIntFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsInt((Long) o);
        }

        return originArray;
    }
}
