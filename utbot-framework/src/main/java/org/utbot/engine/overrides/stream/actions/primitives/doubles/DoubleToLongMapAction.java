package org.utbot.engine.overrides.stream.actions.primitives.doubles;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.DoubleToLongFunction;

public class DoubleToLongMapAction implements StreamAction {
    private final DoubleToLongFunction mapping;

    public DoubleToLongMapAction(DoubleToLongFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsLong((Double) o);
        }

        return originArray;
    }
}
