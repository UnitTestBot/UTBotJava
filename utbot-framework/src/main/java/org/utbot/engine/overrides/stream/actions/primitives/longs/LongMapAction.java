package org.utbot.engine.overrides.stream.actions.primitives.longs;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.LongFunction;
import java.util.function.LongUnaryOperator;

public class LongMapAction implements StreamAction {
    private final LongUnaryOperator mapping;

    public LongMapAction(LongUnaryOperator mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsLong((Long) o);
        }

        return originArray;
    }
}
