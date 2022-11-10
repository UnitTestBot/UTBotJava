package org.utbot.quickcheck.generator;

import org.utbot.quickcheck.internal.DefaultMethodHandleMaker;
import org.utbot.quickcheck.internal.GeometricDistribution;
import org.utbot.quickcheck.internal.generator.SimpleGenerationStatus;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import static java.lang.System.identityHashCode;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.utbot.quickcheck.internal.Reflection.singleAbstractMethodOf;

/**
 * Helper class for creating instances of "functional interfaces".
 */
public final class Lambdas {
    private Lambdas() {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>Creates an instance of a given "functional interface" type, whose
     * single abstract method returns values of the type produced by the given
     * generator. The arguments to the lambda's single method will be used to
     * seed a random generator that will be used to generate the return value
     * of that method.</p>
     *
     * <p>junit-quickcheck uses this to create random values for property
     * parameters whose type is determined to be a
     * {@linkplain FunctionalInterface functional interface}. Custom generators
     * for functional interface types can use this also.</p>
     *
     * @param lambdaType a functional interface type token
     * @param returnValueGenerator a generator for the return type of the
     * functional interface's single method
     * @param status an object to be passed along to the generator that will
     * produce the functional interface's method return value
     * @param <T> the functional interface type token
     * @param <U> the type of the generated return value of the functional
     * interface method
     * @return an instance of the functional interface type, whose single
     * method will return a generated value
     * @throws IllegalArgumentException if {@code lambdaType} is not a
     * functional interface type
     */
    public static <T, U> T makeLambda(
        Class<T> lambdaType,
        org.utbot.quickcheck.generator.Generator<U> returnValueGenerator,
        GenerationStatus status) {

        if (singleAbstractMethodOf(lambdaType) == null) {
            throw new IllegalArgumentException(
                lambdaType + " is not a functional interface type");
        }

        return lambdaType.cast(
            newProxyInstance(
                lambdaType.getClassLoader(),
                new Class<?>[] { lambdaType },
                new LambdaInvocationHandler<>(
                    lambdaType,
                    returnValueGenerator,
                    status.attempts())));
    }

    private static class LambdaInvocationHandler<T, U>
        implements InvocationHandler {

        private final Class<T> lambdaType;
        private final org.utbot.quickcheck.generator.Generator<U> returnValueGenerator;
        private final int attempts;
        private final DefaultMethodHandleMaker methodHandleMaker =
            new DefaultMethodHandleMaker();

        LambdaInvocationHandler(
            Class<T> lambdaType,
            Generator<U> returnValueGenerator,
            Integer attempts) {

            this.lambdaType = lambdaType;
            this.returnValueGenerator = returnValueGenerator;
            this.attempts = attempts;
        }

        @Override public Object invoke(
            Object proxy,
            Method method,
            Object[] args)
            throws Throwable {

            if (Object.class.equals(method.getDeclaringClass()))
                return handleObjectMethod(proxy, method, args);
            if (method.isDefault())
                return handleDefaultMethod(proxy, method, args);

            SourceOfRandomness source = new SourceOfRandomness(new Random());
            source.setSeed(Arrays.hashCode(args));
            GenerationStatus status =
                new SimpleGenerationStatus(
                    new GeometricDistribution(),
                    source,
                    attempts);
            return returnValueGenerator.generate(source, status);
        }

        private Object handleObjectMethod(
            Object proxy,
            Method method,
            Object[] args) {

            if ("equals".equals(method.getName()))
                return proxy == args[0];

            if ("hashCode".equals(method.getName()))
                return identityHashCode(proxy);

            return handleToString();
        }

        private String handleToString() {
            return "a randomly generated instance of " + lambdaType;
        }

        private Object handleDefaultMethod(
            Object proxy,
            Method method,
            Object[] args)
            throws Throwable {

            MethodHandle handle =
                methodHandleMaker.handleForSpecialMethod(method);
            return handle.bindTo(proxy).invokeWithArguments(args);
        }
    }
}
