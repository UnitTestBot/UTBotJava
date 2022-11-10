

package org.utbot.quickcheck.internal.generator;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.internal.Zilch;
import org.utbot.quickcheck.random.SourceOfRandomness;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

public class ZilchGenerator extends Generator<Zilch> {
    public ZilchGenerator() {
        super(Zilch.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(Zilch.INSTANCE, classIdForType(Zilch.class));
    }
}
