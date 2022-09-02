package org.utbot.engine.overrides.stream.actions.primitives.ints;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

public class IntMapAction implements StreamAction {
    private final IntUnaryOperator mapping;

    public IntMapAction(IntUnaryOperator mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsInt((Integer) o);
        }

        return originArray;
    }
}
