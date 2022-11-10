

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.UnaryOperator;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link UnaryOperator}.
 *
 * @param <T> type of parameter and return type of produced operator
 */
public class UnaryOperatorGenerator<T>
    extends ComponentizedGenerator<UnaryOperator> {

    public UnaryOperatorGenerator() {
        super(UnaryOperator.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(
            UnaryOperator.class,
            componentGenerators().get(0),
            status), classIdForType(UnaryOperator.class));
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }
}
