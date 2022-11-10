package org.utbot.quickcheck.generator;

import org.utbot.framework.plugin.api.UtModel;
import org.utbot.framework.plugin.api.UtNullModel;
import org.utbot.quickcheck.random.SourceOfRandomness;

import static java.util.Arrays.asList;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values for property parameters of type {@code void} or
 * {@link Void}.
 */
public class VoidGenerator extends Generator<Void> {
    public VoidGenerator() {
        super(asList(Void.class, void.class));
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return new UtNullModel(classIdForType(Void.class));
    }

    @Override
    public boolean canRegisterAsType(Class<?> type) {
        return !Object.class.equals(type);
    }
}
