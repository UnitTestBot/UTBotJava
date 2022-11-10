

package org.utbot.quickcheck.generator.java.util.concurrent;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.concurrent.Callable;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@code Callable}.
 *
 * @param <V> the type of the values produced by the generated instances
 */
public class CallableGenerator<V> extends ComponentizedGenerator<Callable> {
    public CallableGenerator() {
        super(Callable.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(
            Callable.class,
            componentGenerators().get(0),
            status), classIdForType(CallableGenerator.class));
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }
}
