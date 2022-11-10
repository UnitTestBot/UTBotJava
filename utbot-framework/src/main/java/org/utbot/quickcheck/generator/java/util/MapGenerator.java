

package org.utbot.quickcheck.generator.java.util;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;
import org.utbot.quickcheck.generator.*;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.*;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getObjectClassId;
import static org.utbot.framework.plugin.api.util.IdUtilKt.methodId;
import static org.utbot.quickcheck.internal.Ranges.Type.INTEGRAL;
import static org.utbot.quickcheck.internal.Ranges.checkRange;
import static org.utbot.quickcheck.internal.Reflection.findConstructor;
import static org.utbot.quickcheck.internal.Reflection.instantiate;

/**
 * <p>Base class for generators of {@link Map}s.</p>
 *
 * <p>The generated map has a number of entries limited by
 * {@link GenerationStatus#size()}, or else by the attributes of a {@link Size}
 * marking. The individual keys and values will have types corresponding to the
 * property parameter's type arguments.</p>
 *
 * @param <T> the type of map generated
 */
public abstract class MapGenerator<T extends Map>
    extends ComponentizedGenerator<T> {

    private Size sizeRange;
    private boolean distinct;

    protected MapGenerator(Class<T> type) {
        super(type);
    }

    /**
     * <p>Tells this generator to add key-value pairs to the generated map a
     * number of times within a specified minimum and/or maximum, inclusive,
     * chosen with uniform distribution.</p>
     *
     * <p>Note that maps disallow duplicate keys, so the number of pairs added
     * may not be equal to the map's {@link Map#size()}.</p>
     *
     * @param size annotation that gives the size constraints
     */
    public void configure(Size size) {
        this.sizeRange = size;
        checkRange(INTEGRAL, size.min(), size.max());
    }

    /**
     * Tells this generator to add entries whose keys are distinct from
     * each other.
     *
     * @param distinct Keys of generated entries will be distinct if this
     * param is not null
     */
    public void configure(Distinct distinct) {
        this.distinct = distinct != null;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int size = size(random, status);

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final ClassId classId = classIdForType(types().get(0));
        final Generator<?> keyGenerator = componentGenerators().get(0);
        final Generator<?> valueGenerator = componentGenerators().get(1);

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
                    final ExecutableId putMethodId = methodId(classId, "put", getObjectClassId(), getObjectClassId(), getObjectClassId());

                    int i = 0;
                    while (i < size) {
                        final UtModel key = keyGenerator.generate(random, status);
                        final UtModel value = valueGenerator.generate(random, status);
                        if (!okToAdd(key, value)) continue;
                        i++;
                        modificationChain.add(new UtExecutableCallModel(a, putMethodId, List.of(key, value)));
                    }
                    return modificationChain;
                }
        );



        return generatedModel;
    }

    @Override public int numberOfNeededComponents() {
        return 2;
    }

    protected boolean okToAdd(Object key, Object value) {
        return true;
    }

    private int size(SourceOfRandomness random, GenerationStatus status) {
        return sizeRange != null
            ? random.nextInt(sizeRange.min(), sizeRange.max())
            : status.size();
    }

}
