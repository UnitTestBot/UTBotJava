

package org.utbot.quickcheck.internal.generator;

import org.javaruntype.type.TypeParameter;
import org.utbot.quickcheck.generator.*;
import org.utbot.quickcheck.internal.ParameterTypeContext;
import org.utbot.quickcheck.internal.Weighted;
import org.utbot.quickcheck.internal.Zilch;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.utbot.quickcheck.internal.Items.choose;
import static org.utbot.quickcheck.internal.Reflection.*;

public class GeneratorRepository implements Generators {
    private static final Set<String> NULLABLE_ANNOTATIONS =
        unmodifiableSet(
            Stream.of(
                "javax.annotation.Nullable", // JSR-305
                NullAllowed.class.getCanonicalName())
                .collect(toSet()));

    private final SourceOfRandomness random;

    private final Map<Class<?>, Set<Generator<?>>> generators;

    public GeneratorRepository(SourceOfRandomness random) {
        this(random, new HashMap<>());
    }

    private GeneratorRepository(
        SourceOfRandomness random,
        Map<Class<?>, Set<Generator<?>>> generators) {

        this.random = random;
        this.generators = generators;
    }

    public Map<Class<?>, Set<Generator<?>>> getGenerators() {
        return generators;
    }

    public void addUserClassGenerator(Class<?> forClass, Generator<?> source) {
        generators.put(forClass, Set.of(source));
    }

    public void removeGenerator(Class<?> forClass) {
        generators.remove(forClass);
    }

    public void removeGeneratorForObjectClass() {
        generators.remove(Object.class);
    }

    public GeneratorRepository register(Generator<?> source) {
        registerTypes(source);
        return this;
    }

    public GeneratorRepository register(Iterable<Generator<?>> source) {
        for (Generator<?> each : source)
            registerTypes(each);

        return this;
    }

    private void registerTypes(Generator<?> generator) {
        for (Class<?> each : generator.types())
            registerHierarchy(each, generator);
    }

    private void registerHierarchy(Class<?> type, Generator<?> generator) {
        maybeRegisterGeneratorForType(type, generator);

        if (type.getSuperclass() != null)
            registerHierarchy(type.getSuperclass(), generator);
        else if (type.isInterface())
            registerHierarchy(Object.class, generator);

        for (Class<?> each : type.getInterfaces())
            registerHierarchy(each, generator);
    }

    private void maybeRegisterGeneratorForType(
        Class<?> type,
        Generator<?> generator) {

        if (generator.canRegisterAsType(type))
            registerGeneratorForType(type, generator);
    }

    private void registerGeneratorForType(
        Class<?> type,
        Generator<?> generator) {

        Set<Generator<?>> forType =
            generators.computeIfAbsent(type, k -> new LinkedHashSet<>());

        forType.add(generator);
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Generator<T> type(
        Class<T> type,
        Class<?>... componentTypes) {

        Generator<T> generator =
            (Generator<T>) produceGenerator(
                ParameterTypeContext.forClass(type));
        generator.addComponentGenerators(
            Arrays.stream(componentTypes).map(c -> type(c)).collect(toList()));
        return generator;
    }

    @Override public Generator<?> parameter(Parameter parameter) {
        return produceGenerator(
            ParameterTypeContext.forParameter(parameter).annotate(parameter));
    }

    @Override public Generator<?> field(Field field) {
        return produceGenerator(
            ParameterTypeContext.forField(field).annotate(field));
    }

    @Override public final Generators withRandom(SourceOfRandomness other) {
        return new GeneratorRepository(other, this.generators);
    }

    public Generator<?> produceGenerator(ParameterTypeContext parameter) {
        Generator<?> generator = generatorFor(parameter);

        if (!isPrimitiveType(parameter.annotatedType().getType())
            && hasNullableAnnotation(parameter.annotatedElement())) {

            generator = new NullableGenerator<>(generator);
        }

        generator.provide(this);
        generator.configure(parameter.annotatedType());
        if (parameter.topLevel())
            generator.configure(parameter.annotatedElement());

        return generator;
    }

    public Generator<?> generatorFor(ParameterTypeContext parameter) {
        if (!parameter.explicitGenerators().isEmpty())
            return composeWeighted(parameter, parameter.explicitGenerators());
        if (parameter.isArray())
            return generatorForArrayType(parameter);
        if (parameter.isEnum())
            return new EnumGenerator(parameter.getRawClass());

        return compose(parameter, matchingGenerators(parameter));
    }

    private Generator<?> generatorForArrayType(
        ParameterTypeContext parameter) {

        ParameterTypeContext component = parameter.arrayComponentContext();
        return new ArrayGenerator(
            component.getRawClass(),
            generatorFor(component));
    }

    private List<Generator<?>> matchingGenerators(
        ParameterTypeContext parameter) {

        List<Generator<?>> matches = new ArrayList<>();

        if (!hasGeneratorsFor(parameter)) {
            maybeAddGeneratorByNamingConvention(parameter, matches);
            maybeAddLambdaGenerator(parameter, matches);
            maybeAddMarkerInterfaceGenerator(parameter, matches);
        } else {
            maybeAddGeneratorsFor(parameter, matches);
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot find generator for " + parameter.name()
                + " of type " + parameter.type().getTypeName());
        }

        return matches;
    }

    private void maybeAddGeneratorByNamingConvention(
        ParameterTypeContext parameter,
        List<Generator<?>> matches) {

        Class<?> genClass;
        try {
            genClass =
                Class.forName(parameter.getRawClass().getName() + "Gen");
        } catch (ClassNotFoundException noGenObeyingConvention) {
            return;
        }

        if (Generator.class.isAssignableFrom(genClass)) {
            try {
                Generator<?> generator = (Generator<?>) genClass.newInstance();
                if (generator.types().contains(parameter.getRawClass())) {
                    matches.add(generator);
                }
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IllegalStateException(
                    "Cannot instantiate " + genClass.getName()
                        + " using default constructor");
            }
        }
    }

    private void maybeAddLambdaGenerator(
        ParameterTypeContext parameter,
        List<Generator<?>> matches) {

        Method method = singleAbstractMethodOf(parameter.getRawClass());
        if (method != null) {
            ParameterTypeContext returnType =
                parameter.methodReturnTypeContext(method);
            Generator<?> returnTypeGenerator = generatorFor(returnType);

            matches.add(
                new LambdaGenerator<>(
                    parameter.getRawClass(),
                    returnTypeGenerator));
        }
    }

    private void maybeAddMarkerInterfaceGenerator(
        ParameterTypeContext parameter,
        List<Generator<?>> matches) {

        Class<?> rawClass = parameter.getRawClass();
        if (isMarkerInterface(rawClass)) {
            matches.add(
                new MarkerInterfaceGenerator<>(parameter.getRawClass()));
        }
    }

    private void maybeAddGeneratorsFor(
        ParameterTypeContext parameter,
        List<Generator<?>> matches) {

        List<Generator<?>> candidates = generatorsFor(parameter);
        List<TypeParameter<?>> typeParameters = parameter.getTypeParameters();

        if (typeParameters.isEmpty()) {
            matches.addAll(candidates);
        } else {
            for (Generator<?> each : candidates) {
                if (each.canGenerateForParametersOfTypes(typeParameters))
                    matches.add(each);
            }
        }
    }

    private Generator<?> compose(
        ParameterTypeContext parameter,
        List<Generator<?>> matches) {

        List<Weighted<Generator<?>>> weightings =
            matches.stream()
                .map(g -> new Weighted<Generator<?>>(g, 1))
                .collect(toList());

        return composeWeighted(parameter, weightings);
    }

    private Generator<?> composeWeighted(
        ParameterTypeContext parameter,
        List<Weighted<Generator<?>>> matches) {

        List<Generator<?>> forComponents = new ArrayList<>();
        for (ParameterTypeContext c : parameter.typeParameterContexts(random))
            forComponents.add(generatorFor(c));

        for (Weighted<Generator<?>> each : matches)
            applyComponentGenerators(each.item, forComponents);

        return matches.size() == 1
            ? matches.get(0).item
            : new CompositeGenerator(matches);
    }

    private void applyComponentGenerators(
        Generator<?> generator,
        List<Generator<?>> componentGenerators) {

        if (!generator.hasComponents())
            return;

        if (componentGenerators.isEmpty()) {
            List<Generator<?>> substitutes = new ArrayList<>();
            Generator<?> zilch =
                generatorFor(
                    ParameterTypeContext.forClass(Zilch.class)
                        .allowMixedTypes(true));
            for (int i = 0; i < generator.numberOfNeededComponents(); ++i) {
                substitutes.add(zilch);
            }

            generator.addComponentGenerators(substitutes);
        } else {
            generator.addComponentGenerators(componentGenerators);
        }
    }

    private List<Generator<?>> generatorsFor(ParameterTypeContext parameter) {
        Set<Generator<?>> matches = generators.get(parameter.getRawClass());

        if (!parameter.allowMixedTypes()) {
            Generator<?> match = choose(matches, random);
            matches = new HashSet<>();
            matches.add(match);
        }

        List<Generator<?>> copies = new ArrayList<>();
        for (Generator<?> each : matches) {
            copies.add(each.copy());
        }
        return copies;
    }

    private boolean hasGeneratorsFor(ParameterTypeContext parameter) {
        return generators.get(parameter.getRawClass()) != null;
    }

    private static boolean isPrimitiveType(Type type) {
        return type instanceof Class<?> && ((Class<?>) type).isPrimitive();
    }

    private static boolean hasNullableAnnotation(AnnotatedElement element) {
        return element != null
            && Arrays.stream(element.getAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getCanonicalName)
                .anyMatch(NULLABLE_ANNOTATIONS::contains);
    }
}
