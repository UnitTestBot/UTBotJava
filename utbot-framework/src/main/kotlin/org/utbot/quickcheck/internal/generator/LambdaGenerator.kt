

package org.utbot.quickcheck.internal.generator;

import org.utbot.framework.plugin.api.UtModel;
import org.utbot.framework.plugin.api.UtNullModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

public class LambdaGenerator<T, U> extends Generator<T> {
    private final Class<T> lambdaType;
    private final Generator<U> returnValueGenerator;

    LambdaGenerator(Class<T> lambdaType, Generator<U> returnValueGenerator) {
        super(lambdaType);

        this.lambdaType = lambdaType;
        this.returnValueGenerator = returnValueGenerator;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return new UtNullModel(classIdForType(Object.class));//makeLambda(lambdaType, returnValueGenerator, status);
    }
}
