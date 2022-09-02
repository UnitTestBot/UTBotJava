package org.utbot.engine.overrides.stream.actions.primitives.ints;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.IntToDoubleFunction;

public class IntToDoubleMapAction implements StreamAction {
    private final IntToDoubleFunction mapping;

    public IntToDoubleMapAction(IntToDoubleFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsDouble((Integer) o);
        }

        return originArray;
    }
}
