

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.BitSet;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link BitSet}.
 */
public class BitSetGenerator extends Generator<BitSet> {
    public BitSetGenerator() {
        super(BitSet.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int size = status.size();

        BitSet bits = new BitSet(size);
        for (int i = 0; i < size; ++i) {
            bits.set(i, random.nextBoolean());
        }

        return UtModelGenerator.getUtModelConstructor().construct(bits, classIdForType(BitSet.class));
    }

}
