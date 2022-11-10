

package org.utbot.quickcheck.generator.java.nio.charset;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.plugin.api.util.IdUtilKt;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.Charset.availableCharsets;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getStringClassId;

/**
 * Produces values of type {@link Charset}.
 */
public class CharsetGenerator extends Generator<Charset> {
    public CharsetGenerator() {
        super(Charset.class);
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();

        final String charsetName = random.choose(availableCharsets().keySet());
        final UtModel charsetNameModel = modelConstructor.construct(charsetName, getStringClassId());

        final Method charsetForName;
        try {
            charsetForName = Charset.class.getMethod("forName", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        final ClassId charsetClassId = classIdForType(Charset.class);
        final ExecutableId charsetForNameId = IdUtilKt.getExecutableId(charsetForName);

        final int modelId = modelConstructor.computeUnusedIdAndUpdate();

        return new UtAssembleModel(
                modelId,
                charsetClassId,
                charsetForNameId.getName() + "#" + modelId,
                new UtExecutableCallModel(null, charsetForNameId, List.of(charsetNameModel)),
                null,
                (a) -> List.of()
        );
    }
}
