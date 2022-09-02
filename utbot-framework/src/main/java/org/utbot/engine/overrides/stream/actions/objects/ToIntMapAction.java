package org.utbot.engine.overrides.stream.actions.objects;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.ToIntFunction;

public class ToIntMapAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final ToIntFunction mapping;

    @SuppressWarnings("rawtypes")
    public ToIntMapAction(ToIntFunction mapping) {
        this.mapping = mapping;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsInt(o);
        }

        return originArray;
    }
}
