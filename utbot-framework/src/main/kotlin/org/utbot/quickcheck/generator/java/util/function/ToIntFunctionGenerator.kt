

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.ToIntFunction;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link ToIntFunction}.
 *
 * @param <T> type of parameter of produced function
 */
public class ToIntFunctionGenerator<T>
    extends ComponentizedGenerator<ToIntFunction> {

    private Generator<Integer> generator;

    public ToIntFunctionGenerator() {
        super(ToIntFunction.class);
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        generator = gen().type(int.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(ToIntFunction.class, generator, status), classIdForType(ToIntFunction.class));
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }
}
