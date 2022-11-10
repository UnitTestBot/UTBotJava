

package org.utbot.quickcheck.generator.java.util;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;

import org.utbot.framework.plugin.api.util.IdUtilKt;
import org.utbot.quickcheck.generator.*;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Ranges.Type.INTEGRAL;
import static org.utbot.quickcheck.internal.Ranges.checkRange;

/**
 * <p>Base class for generators of {@link Collection}s.</p>
 *
 * <p>The generated collection has a number of elements limited by
 * {@link GenerationStatus#size()}, or else by the attributes of a {@link Size}
 * marking. The individual elements will have a type corresponding to the
 * collection's type argument.</p>
 *
 * @param <T> the type of collection generated
 */
public abstract class CollectionGenerator<T extends Collection>
        extends ComponentizedGenerator<T> {

    private Size sizeRange;
    private boolean distinct;

    protected CollectionGenerator(Class<T> type) {
        super(type);
    }

    /**
     * <p>Tells this generator to add elements to the generated collection
     * a number of times within a specified minimum and/or maximum, inclusive,
     * chosen with uniform distribution.</p>
     *
     * <p>Note that some kinds of collections disallow duplicates, so the
     * number of elements added may not be equal to the collection's
     * {@link Collection#size()}.</p>
     *
     * @param size annotation that gives the size constraints
     */
    public void configure(Size size) {
        this.sizeRange = size;
        checkRange(INTEGRAL, size.min(), size.max());
    }

    /**
     * Tells this generator to add elements which are distinct from each other.
     *
     * @param distinct Generated elements will be distinct if this param is
     *                 not null
     */
    public void configure(Distinct distinct) {
        setDistinct(distinct != null);
    }

    protected final void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

//        UtCompositeModel res = new UtCompositeModel(
//
//        )
        UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        Class<T> collectionType = types().get(0);
        ClassId collectionClassId = classIdForType(collectionType);
        ExecutableId collectionConstructorId = new ConstructorId(collectionClassId, new ArrayList<>());
        int genId = modelConstructor.computeUnusedIdAndUpdate();


        UtAssembleModel collectionAssembleModel = new UtAssembleModel(
                genId,
                collectionClassId,
                collectionConstructorId.getName() + "#" + genId,
                new UtExecutableCallModel(null, collectionConstructorId, List.of()),
                null,
                (a) -> {
                    ArrayList<UtStatementModel> modificationChain = new ArrayList<>();
                    int size = size(random, status);

                    Generator<?> generator = componentGenerators().get(0);
                    Method m;
                    try {
                        m = Collection.class.getMethod("add", Object.class);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                    MethodId methodId = IdUtilKt.getExecutableId(m);

                    //java.util.Collections::class.java.methodCall
                    for (int i = 0; i < size; ++i) {
                        UtModel item = generator.generate(random, status);
                        modificationChain.add(new UtExecutableCallModel(a, methodId, List.of(item)));
                    }
                    return modificationChain;
                }
        );

        return collectionAssembleModel;
    }

    @Override
    public int numberOfNeededComponents() {
        return 1;
    }

    private int size(SourceOfRandomness random, GenerationStatus status) {
        return sizeRange != null
                ? random.nextInt(sizeRange.min(), sizeRange.max())
                : status.size();
    }

}
