

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;

import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.IntFunction;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link IntFunction}.
 *
 * @param <R> return type of produced function
 */
public class IntFunctionGenerator<R>
    extends ComponentizedGenerator<IntFunction> {

    public IntFunctionGenerator() {
        super(IntFunction.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(
            IntFunction.class,
            componentGenerators().get(0),
            status), classIdForType(IntFunction.class));
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }
}
