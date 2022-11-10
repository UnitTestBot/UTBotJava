

package org.utbot.quickcheck.internal.generator;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

public class EnumGenerator extends Generator<Enum> {
    private final Class<?> enumType;

    EnumGenerator(Class<?> enumType) {
        super(Enum.class);

        this.enumType = enumType;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        Object[] values = enumType.getEnumConstants();
        int index = random.nextInt(0, values.length - 1);
        return UtModelGenerator.getUtModelConstructor().construct(values[index], classIdForType(Enum.class));
    }

}
