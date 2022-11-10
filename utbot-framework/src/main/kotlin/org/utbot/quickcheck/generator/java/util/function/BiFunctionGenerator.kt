

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.BiFunction;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link BiFunction}.
 *
 * @param <T> type of first parameter of produced function
 * @param <U> type of second parameter of produced function
 * @param <R> return type of produced function
 */
public class BiFunctionGenerator<T, U, R>
    extends ComponentizedGenerator<BiFunction> {

    public BiFunctionGenerator() {
        super(BiFunction.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(
            BiFunction.class,
            componentGenerators().get(2),
            status), classIdForType(BiFunction.class));
    }

    @Override public int numberOfNeededComponents() {
        return 3;
    }
}
