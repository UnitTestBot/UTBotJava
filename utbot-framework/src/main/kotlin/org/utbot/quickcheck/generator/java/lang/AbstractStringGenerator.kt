package org.utbot.quickcheck.generator.java.lang;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import static org.utbot.framework.plugin.api.util.IdUtilKt.getStringClassId;

/**
 * <p>Base class for generators of values of type {@link String}.</p>
 *
 * <p>The generated values will have {@linkplain String#length()} decided by
 * {@link GenerationStatus#size()}.</p>
 */
public abstract class AbstractStringGenerator extends Generator<String> {
    protected AbstractStringGenerator() {
        super(String.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {
        return UtModelGenerator.getUtModelConstructor().construct(generateValue(random, status), getStringClassId());
    }

    public String generateValue( SourceOfRandomness random,
                                 GenerationStatus status){
        int[] codePoints = new int[status.size()];

        for (int i = 0; i < codePoints.length; ++i)
            codePoints[i] = nextCodePoint(random);
        return new String(codePoints, 0, codePoints.length);
    }

    protected abstract int nextCodePoint(SourceOfRandomness random);

}
