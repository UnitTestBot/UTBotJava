package org.utbot.engine.overrides.stream.actions.objects;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.ToLongFunction;

public class ToLongMapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final ToLongFunction mapping;

    @SuppressWarnings("rawtypes")
    public ToLongMapAction(ToLongFunction mapping) {
        this.mapping = mapping;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsLong(o);
        }

        return originArray;
    }
}
