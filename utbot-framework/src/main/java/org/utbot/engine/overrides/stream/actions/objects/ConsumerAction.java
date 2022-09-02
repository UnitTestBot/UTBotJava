package org.utbot.engine.overrides.stream.actions.objects;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.Consumer;

public class ConsumerAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final Consumer action;

    @SuppressWarnings("rawtypes")
    public ConsumerAction(Consumer action) {
        this.action = action;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] applyAction(Object[] originArray) {
        for (Object element : originArray) {
            action.accept(element);
        }

        return originArray;
    }
}
