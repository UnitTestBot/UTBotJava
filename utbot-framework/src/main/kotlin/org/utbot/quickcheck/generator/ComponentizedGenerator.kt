package org.utbot.quickcheck.generator;

import org.javaruntype.type.TypeParameter;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.utbot.quickcheck.internal.Reflection.annotatedComponentTypes;

/**
 * Produces values for property parameters of types that have parameterizations
 * that would also need generation, such as collections, maps, and predicates.
 *
 * @param <T> type of property parameter to apply this generator's values to
 */
public abstract class ComponentizedGenerator<T> extends Generator<T> {

    private final List<Generator<?>> components = new ArrayList<>();

    /**
     * @param type class token for type of property parameter this generator
     * is applicable to
     */
    protected ComponentizedGenerator(Class<T> type) {
        super(type);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generators of this type do not get called upon to generate values
     * for parameters of type {@link Object}.</p>
     */
    @Override public boolean canRegisterAsType(Class<?> type) {
        return !Object.class.equals(type);
    }

    @Override public final boolean hasComponents() {
        return true;
    }

    @Override public void addComponentGenerators(
        List<org.utbot.quickcheck.generator.Generator<?>> newComponents) {

        if (newComponents.size() != numberOfNeededComponents()) {
            throw new IllegalArgumentException(
                String.format(
                    "Needed %d components for %s, but got %d",
                    numberOfNeededComponents(),
                    getClass(),
                    newComponents.size()));
        }

        components.clear();
        components.addAll(newComponents);
    }

    @Override public boolean canGenerateForParametersOfTypes(
        List<TypeParameter<?>> typeParameters) {

        return numberOfNeededComponents() == typeParameters.size();
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        for (org.utbot.quickcheck.generator.Generator<?> each : components) {
            each.provide(provided);
        }
    }

    @Override public void configure(AnnotatedType annotatedType) {
        super.configure(annotatedType);

        List<AnnotatedType> annotatedComponentTypes =
            annotatedComponentTypes(annotatedType);

        if (annotatedComponentTypes.size() == components.size()) {
            for (int i = 0; i < components.size(); ++i) {
                components.get(i).configure(annotatedComponentTypes.get(i));
            }
        }
    }

    /**
     * @return this generator's component generators
     */
    public List<Generator<?>> componentGenerators() {
        return unmodifiableList(components);
    }
}
