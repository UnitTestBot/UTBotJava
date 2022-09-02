package org.utbot.engine.overrides.stream.actions.primitives.ints;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

public class IntToObjMapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final IntFunction mapping;

    @SuppressWarnings("rawtypes")
    public IntToObjMapAction(IntFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.apply((Integer) o);
        }

        return originArray;
    }
}
