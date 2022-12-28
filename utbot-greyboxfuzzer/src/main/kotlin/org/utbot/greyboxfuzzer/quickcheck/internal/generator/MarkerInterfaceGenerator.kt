package org.utbot.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.internal.DefaultMethodHandleMaker
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MarkerInterfaceGenerator(private val markerType: Class<*>) : Generator(markerType) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            markerType.cast(
                Proxy.newProxyInstance(
                    markerType.classLoader, arrayOf(markerType),
                    MarkerInvocationHandler(markerType)
                )
            ),
            classIdForType(markerType)
        )
    }

    private class MarkerInvocationHandler(private val markerType: Class<*>) : InvocationHandler {
        private val methodHandleMaker = DefaultMethodHandleMaker()

        override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any? {
            if (Any::class.java == method.declaringClass) return handleObjectMethod(proxy, method, args)
            return if (method.isDefault) handleDefaultMethod(proxy, method, args) else null
        }

        private fun handleObjectMethod(
            proxy: Any,
            method: Method,
            args: Array<Any>
        ): Any {
            if ("equals" == method.name) return proxy === args[0]
            return if ("hashCode" == method.name) System.identityHashCode(proxy) else handleToString()
        }

        private fun handleDefaultMethod(
            proxy: Any,
            method: Method,
            args: Array<Any>
        ): Any {
            val handle = methodHandleMaker.handleForSpecialMethod(method)
            return handle.bindTo(proxy).invokeWithArguments(*args)
        }

        private fun handleToString(): String {
            return "a synthetic instance of $markerType"
        }
    }
}