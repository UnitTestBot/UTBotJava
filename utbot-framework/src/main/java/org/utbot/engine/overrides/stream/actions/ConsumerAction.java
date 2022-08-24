package org.utbot.engine.overrides.stream.actions;

import java.util.function.Consumer;

public class ConsumerAction implements StreamAction {
    private final Consumer<Object> action;

    public ConsumerAction(Consumer<Object> action) {
        this.action = action;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        for (Object element : originArray) {
            action.accept(element);
        }

        return originArray;
    }
}
