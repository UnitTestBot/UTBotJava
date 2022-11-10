
package org.utbot.quickcheck.generator.java.lang;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import static java.util.Collections.singletonList;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getBooleanWrapperClassId;

/**
 * Produces values of type {@code boolean} or {@link Boolean}.
 */
public class BooleanGenerator extends Generator<Boolean> {
    public BooleanGenerator() {
        super(singletonList(Boolean.class));
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(random.nextBoolean(), getBooleanWrapperClassId());
    }

}
