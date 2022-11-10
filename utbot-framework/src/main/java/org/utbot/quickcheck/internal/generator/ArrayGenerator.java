

package org.utbot.quickcheck.internal.generator;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.ClassId;
import org.utbot.framework.plugin.api.UtArrayModel;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.framework.plugin.api.util.IdUtilKt;
import org.utbot.quickcheck.generator.*;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.AnnotatedType;
import java.util.*;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Ranges.Type.INTEGRAL;
import static org.utbot.quickcheck.internal.Ranges.checkRange;
import static org.utbot.quickcheck.internal.Reflection.annotatedComponentTypes;

public class ArrayGenerator extends Generator<Object> {
    private final Class<?> componentType;
    private final Generator<?> component;

    private Size lengthRange;
    private boolean distinct;

    ArrayGenerator(Class<?> componentType, Generator<?> component) {
        super(Object.class);

        this.componentType = componentType;
        this.component = component;
    }

    public Generator<?> getComponent() {
        return component;
    }

    /**
     * Tells this generator to produce values with a length within a specified
     * minimum and/or maximum, inclusive, chosen with uniform distribution.
     *
     * @param size annotation that gives the length constraints
     */
    public void configure(Size size) {
        this.lengthRange = size;
        checkRange(INTEGRAL, size.min(), size.max());
    }

    /**
     * Tells this generator to produce values which are distinct from each
     * other.
     *
     * @param distinct Generated values will be distinct if this param is not
     * null.
     */
    public void configure(Distinct distinct) {
        this.distinct = distinct != null;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int length = length(random, status);
        final ClassId componentTypeId = classIdForType(componentType);

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final int modelId = modelConstructor.computeUnusedIdAndUpdate();
        final Map<Integer, UtModel> stores = new HashMap<>();
        final UtModel generatedModel = new UtArrayModel(
                modelId, getClassIdForArrayType(componentType), length, IdUtilKt.defaultValueModel(componentTypeId), stores
        );

        for (int i = 0; i < length; ++i) {
            final UtModel item = component.generate(random, status);
            stores.put(i, item);
        }

        return generatedModel;
    }
    private ClassId getClassIdForArrayType(Class<?> componentType) {
        if (int.class.equals(componentType)) {
            return new ClassId("[i", classIdForType(int.class));
        } else if (boolean.class.equals(componentType)) {
            return new ClassId("[z", classIdForType(boolean.class));
        } else if (byte.class.equals(componentType)) {
            return new ClassId("[b", classIdForType(byte.class));
        } else if (char.class.equals(componentType)) {
            return new ClassId("[c", classIdForType(char.class));
        } else if (double.class.equals(componentType)) {
            return new ClassId("[d", classIdForType(double.class));
        } else if (float.class.equals(componentType)) {
            return new ClassId("[f", classIdForType(float.class));
        } else if (long.class.equals(componentType)) {
            return new ClassId("[j", classIdForType(long.class));
        } else if (short.class.equals(componentType)) {
            return new ClassId("[s", classIdForType(short.class));
        } else {
            return new ClassId("[L", classIdForType(componentType));
        }
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        component.provide(provided);
    }

    @Override public void configure(AnnotatedType annotatedType) {
        super.configure(annotatedType);

        List<AnnotatedType> annotated = annotatedComponentTypes(annotatedType);
        if (!annotated.isEmpty()) {
            component.configure(annotated.get(0));
        }
    }

    private int length(SourceOfRandomness random, GenerationStatus status) {
        return lengthRange != null
            ? random.nextInt(lengthRange.min(), lengthRange.max())
            : status.size();
    }

}
