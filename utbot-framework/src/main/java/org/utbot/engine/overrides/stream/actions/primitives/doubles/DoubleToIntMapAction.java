package org.utbot.engine.overrides.stream.actions.primitives.doubles;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.DoubleToIntFunction;

public class DoubleToIntMapAction implements StreamAction {
    private final DoubleToIntFunction mapping;

    public DoubleToIntMapAction(DoubleToIntFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsInt((Double) o);
        }

        return originArray;
    }
}
