

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.List;
import java.util.Optional;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getObjectClassId;
import static org.utbot.framework.plugin.api.util.IdUtilKt.methodId;

/**
 * Produces values of type {@link Optional}.
 */
public class OptionalGenerator extends ComponentizedGenerator<Optional> {
    public OptionalGenerator() {
        super(Optional.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        double trial = random.nextDouble();
        if (trial < 0.25) {
            return UtModelGenerator.getUtModelConstructor().construct(Optional.empty(), classIdForType(Optional.class));
        }

        final UtModel value = componentGenerators().get(0).generate(random, status);

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final ClassId classId = classIdForType(Optional.class);
        final ExecutableId constructorId = methodId(classId, "of", classId, getObjectClassId());

        final int generatedModelId = modelConstructor.computeUnusedIdAndUpdate();
        return new UtAssembleModel(
                generatedModelId,
                classId,
                constructorId.getName() + "#" + generatedModelId,
                new UtExecutableCallModel(null, constructorId, List.of(value)),
                null,
                (a) -> List.of()
        );
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }

}
