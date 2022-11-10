

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.ToDoubleFunction;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link ToDoubleFunction}.
 *
 * @param <T> type of parameter of produced function
 */
public class ToDoubleFunctionGenerator<T>
    extends ComponentizedGenerator<ToDoubleFunction> {

    private Generator<Double> generator;

    public ToDoubleFunctionGenerator() {
        super(ToDoubleFunction.class);
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        generator = gen().type(double.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(ToDoubleFunction.class, generator, status), classIdForType(ToDoubleFunction.class));
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }
}
