

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;

import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.ToLongBiFunction;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link ToLongBiFunction}.
 *
 * @param <T> type of first parameter of produced function
 * @param <U> type of second parameter of produced function
 */
public class ToLongBiFunctionGenerator<T, U>
    extends ComponentizedGenerator<ToLongBiFunction> {

    private Generator<Long> generator;

    public ToLongBiFunctionGenerator() {
        super(ToLongBiFunction.class);
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        generator = gen().type(long.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(ToLongBiFunction.class, generator, status), classIdForType(ToLongBiFunction.class));
    }

    @Override public int numberOfNeededComponents() {
        return 2;
    }
}
