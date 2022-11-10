

package org.utbot.quickcheck.generator.java.util.function;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.function.Supplier;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.generator.Lambdas.makeLambda;

/**
 * Produces values of type {@code Supplier}.
 *
 * @param <T> the type of the values produced by the generated instances
 */
public class SupplierGenerator<T> extends ComponentizedGenerator<Supplier> {
    public SupplierGenerator() {
        super(Supplier.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(makeLambda(
            Supplier.class,
            componentGenerators().get(0),
            status), classIdForType(Supplier.class));
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }
}
