package org.utbot.engine.overrides.stream.actions.primitives.doubles;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.DoubleFunction;

public class DoubleToObjMapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final DoubleFunction mapping;

    @SuppressWarnings("rawtypes")
    public DoubleToObjMapAction(DoubleFunction mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.apply((Double) o);
        }

        return originArray;
    }
}
