package org.utbot.engine.overrides.stream.actions.primitives.doubles;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.DoubleConsumer;

public class DoubleConsumerAction implements StreamAction {
    private final DoubleConsumer action;

    public DoubleConsumerAction(DoubleConsumer action) {
        this.action = action;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        for (Object element : originArray) {
            action.accept((Double) element);
        }

        return originArray;
    }
}
