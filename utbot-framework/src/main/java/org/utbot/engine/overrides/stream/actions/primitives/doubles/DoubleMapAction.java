package org.utbot.engine.overrides.stream.actions.primitives.doubles;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.function.DoubleUnaryOperator;

public class DoubleMapAction implements StreamAction {
    private final DoubleUnaryOperator mapping;

    public DoubleMapAction(DoubleUnaryOperator mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        int i = 0;
        for (Object o : originArray) {
            originArray[i++] = mapping.applyAsDouble((Double) o);
        }

        return originArray;
    }
}
