package org.utbot.engine.overrides.stream.actions.primitives.longs;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;

public class LongToDoubleMapAction implements StreamAction {
    private final LongToDoubleFunction mapping;

    public LongToDoubleMapAction(LongToDoubleFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsDouble((Long) o);
        }

        return originArray;
    }
}
