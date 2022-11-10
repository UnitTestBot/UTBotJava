
package org.utbot.quickcheck.internal.generator;

import org.utbot.framework.plugin.api.UtModel;
import org.utbot.framework.plugin.api.UtNullModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.generator.NullAllowed;
import org.utbot.quickcheck.random.SourceOfRandomness;
import org.javaruntype.type.TypeParameter;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

class NullableGenerator<T> extends Generator<T> {
    private final Generator<T> delegate;
    private double probabilityOfNull =
        (Double) defaultValueOf(NullAllowed.class, "probability");

    NullableGenerator(Generator<T> delegate) {
        super(delegate.types());

        this.delegate = delegate;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return random.nextFloat(0, 1) < probabilityOfNull
            ? new UtNullModel(classIdForType(types().get(0)))
            : delegate.generate(random, status);
    }

    @Override public boolean canRegisterAsType(Class<?> type) {
        return delegate.canRegisterAsType(type);
    }

    @Override public boolean hasComponents() {
        return delegate.hasComponents();
    }

    @Override public int numberOfNeededComponents() {
        return delegate.numberOfNeededComponents();
    }

    @Override public void addComponentGenerators(
        List<Generator<?>> newComponents) {

        delegate.addComponentGenerators(newComponents);
    }

    @Override public boolean canGenerateForParametersOfTypes(
        List<TypeParameter<?>> typeParameters) {

        return delegate.canGenerateForParametersOfTypes(typeParameters);
    }

    @Override public void configure(AnnotatedType annotatedType) {
        Optional.ofNullable(annotatedType.getAnnotation(NullAllowed.class))
            .ifPresent(this::configure);

        delegate.configure(annotatedType);
    }

    @Override public void configure(AnnotatedElement element) {
        delegate.configure(element);
    }

    @Override public void provide(Generators provided) {
        delegate.provide(provided);
    }

    private void configure(NullAllowed allowed) {
        if (allowed.probability() >= 0 && allowed.probability() <= 1) {
            this.probabilityOfNull = allowed.probability();
        } else {
            throw new IllegalArgumentException(
                "NullAllowed probability must be in the range [0, 1]");
        }
    }
}
