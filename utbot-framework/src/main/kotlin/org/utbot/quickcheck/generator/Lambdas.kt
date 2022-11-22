package org.utbot.quickcheck.generator

import org.utbot.quickcheck.internal.DefaultMethodHandleMaker
import org.utbot.quickcheck.internal.GeometricDistribution
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.internal.generator.SimpleGenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Random

/**
 * Helper class for creating instances of "functional interfaces".
 */
class Lambdas private constructor() {
    init {
        throw UnsupportedOperationException()
    }

    private class LambdaInvocationHandler<T> internal constructor(
        private val lambdaType: Class<T>,
        private val returnValueGenerator: Generator,
        private val attempts: Int
    ) : InvocationHandler {
        private val methodHandleMaker = DefaultMethodHandleMaker()

        override fun invoke(
            proxy: Any,
            method: Method,
            args: Array<Any>
        ): Any {
            if (Any::class.java == method.declaringClass) return handleObjectMethod(proxy, method, args)
            if (method.isDefault) return handleDefaultMethod(proxy, method, args)
            val source = SourceOfRandomness(Random())
            source.setSeed(args.contentHashCode().toLong())
            val status: GenerationStatus = SimpleGenerationStatus(
                GeometricDistribution(),
                source,
                attempts
            )
            return returnValueGenerator.generateImpl(source, status)
        }

        private fun handleObjectMethod(
            proxy: Any,
            method: Method,
            args: Array<Any>
        ): Any {
            if ("equals" == method.name) return proxy === args[0]
            return if ("hashCode" == method.name) System.identityHashCode(proxy) else handleToString()
        }

        private fun handleToString(): String {
            return "a randomly generated instance of $lambdaType"
        }

        private fun handleDefaultMethod(
            proxy: Any,
            method: Method,
            args: Array<Any>
        ): Any {
            val handle = methodHandleMaker.handleForSpecialMethod(method)
            return handle.bindTo(proxy).invokeWithArguments(*args)
        }
    }

    companion object {
        /**
         *
         * Creates an instance of a given "functional interface" type, whose
         * single abstract method returns values of the type produced by the given
         * generator. The arguments to the lambda's single method will be used to
         * seed a random generator that will be used to generate the return value
         * of that method.
         *
         *
         * junit-quickcheck uses this to create random values for property
         * parameters whose type is determined to be a
         * [functional interface][FunctionalInterface]. Custom generators
         * for functional interface types can use this also.
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
         * @throws IllegalArgumentException if `lambdaType` is not a
         * functional interface type
        </U></T> */
        @JvmStatic
        fun <T> makeLambda(
            lambdaType: Class<T>,
            returnValueGenerator: Generator,
            status: GenerationStatus
        ): T {
            requireNotNull(Reflection.singleAbstractMethodOf(lambdaType)) { "$lambdaType is not a functional interface type" }
            return lambdaType.cast(
                Proxy.newProxyInstance(
                    lambdaType.classLoader, arrayOf<Class<*>>(lambdaType),
                    LambdaInvocationHandler(
                        lambdaType,
                        returnValueGenerator,
                        status.attempts()
                    )
                )
            )
        }
    }
}