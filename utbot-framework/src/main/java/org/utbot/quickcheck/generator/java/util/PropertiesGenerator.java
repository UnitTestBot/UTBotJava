package org.utbot.quickcheck.generator.java.util;




import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.java.lang.AbstractStringGenerator;
import org.utbot.quickcheck.generator.java.lang.Encoded;
import org.utbot.quickcheck.generator.java.lang.StringGenerator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.*;

import static java.util.Arrays.asList;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getObjectClassId;
import static org.utbot.framework.plugin.api.util.IdUtilKt.methodId;

/**
 * Produces values of type {@link Properties}.
 */
public class PropertiesGenerator extends Generator<Properties> {
    private AbstractStringGenerator stringGenerator = new StringGenerator();

    public PropertiesGenerator() {
        super(Properties.class);
    }

    public void configure(Encoded.InCharset charset) {
        Encoded encoded = new Encoded();
        encoded.configure(charset);
        stringGenerator = encoded;
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        int size = status.size();

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final ClassId classId = classIdForType(Properties.class);

        final ExecutableId constructorId = new ConstructorId(classId, List.of());
        final int generatedModelId = modelConstructor.computeUnusedIdAndUpdate();

        final UtAssembleModel generatedModel = new UtAssembleModel(
                generatedModelId,
                classId,
                constructorId.getName() + "#" + generatedModelId,
                new UtExecutableCallModel(null, constructorId, List.of()),
                null,
                (a) -> {
                    final List<UtStatementModel> modificationChain = new ArrayList<>();
                    final ExecutableId setPropertyMethodId = methodId(classId, "setProperty", getObjectClassId(), getObjectClassId(), getObjectClassId());

                    for (int i = 0; i < size; i++) {
                        final UtModel key = stringGenerator.generate(random, status);
                        final UtModel value = stringGenerator.generate(random, status);
                        modificationChain.add(new UtExecutableCallModel(a, setPropertyMethodId, List.of(key, value)));
                    }
                    return modificationChain;
                }
        );

        return generatedModel;
    }

    @Override public boolean canRegisterAsType(Class<?> type) {
        Set<Class<?>> exclusions =
            new HashSet<>(
                asList(
                    Object.class,
                    Hashtable.class,
                    Map.class,
                    Dictionary.class));
        return !exclusions.contains(type);
    }

}
