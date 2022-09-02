package org.utbot.engine.overrides.stream.actions.objects;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.ToDoubleFunction;

public class ToDoubleMapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final ToDoubleFunction mapping;

    @SuppressWarnings("rawtypes")
    public ToDoubleMapAction(ToDoubleFunction mapping) {
        this.mapping = mapping;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsDouble(o);
        }

        return originArray;
    }
}
