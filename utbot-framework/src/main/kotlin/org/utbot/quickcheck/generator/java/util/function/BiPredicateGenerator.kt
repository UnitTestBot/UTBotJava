

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.BiPredicate;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@link BiPredicate}.
 *
 * @param <T> type of first parameter of produced predicate
 * @param <U> type of second parameter of produced predicate
 */
public class BiPredicateGenerator<T, U>
    extends ComponentizedGenerator<BiPredicate> {

    private Generator<Boolean> generator;

    public BiPredicateGenerator() {
        super(BiPredicate.class);
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        generator = gen().type(boolean.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(BiPredicate.class, generator, status), classIdForType(BiPredicateGenerator.class));
    }

    @Override public int numberOfNeededComponents() {
        return 2;
    }
}
