package org.utbot.engine.overrides.stream.actions.primitives.ints;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.IntToLongFunction;

public class IntToLongMapAction implements StreamAction {
    private final IntToLongFunction mapping;

    public IntToLongMapAction(IntToLongFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsLong((Integer) o);
        }

        return originArray;
    }
}
