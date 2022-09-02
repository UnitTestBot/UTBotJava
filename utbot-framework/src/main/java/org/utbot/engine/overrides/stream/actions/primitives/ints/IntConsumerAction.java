package org.utbot.engine.overrides.stream.actions.primitives.ints;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.IntConsumer;

public class IntConsumerAction implements StreamAction {
    private final IntConsumer action;

    public IntConsumerAction(IntConsumer action) {
        this.action = action;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        for (Object element : originArray) {
            action.accept((Integer) element);
        }

        return originArray;
    }
}
