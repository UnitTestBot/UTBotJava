package org.utbot.engine.overrides.stream.actions.primitives.longs;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.LongConsumer;

public class LongConsumerAction implements StreamAction {
    private final LongConsumer action;

    public LongConsumerAction(LongConsumer action) {
        this.action = action;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        for (Object element : originArray) {
            action.accept((Long) element);
        }

        return originArray;
    }
}
