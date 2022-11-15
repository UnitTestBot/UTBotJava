package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.common.isPublic
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import kotlin.random.Random

class ConstructorBasedInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val initialGenericContext: GenericsContext?,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val depth: Int
): InstanceGenerator {

    override fun generate(): UtModel? {
        val constructor = chooseRandomConstructor(clazz) ?: return null
        val resolvedConstructor =
            //In case if we can not resolve constructor
            gctx.constructor(constructor).let {
                try {
                    it.toString()
                    it
                } catch (_: Throwable) {
                    try {
                        initialGenericContext?.constructor(constructor)
                    } catch (_: Throwable) {
                        return UtNullModel(clazz.id)
                    }
                }
            }
        return ExecutableInvoker(
            constructor,
            clazz,
            constructor.executableId,
            resolvedConstructor,
            sourceOfRandomness,
            generationStatus,
            depth
        ).invoke()
    }

    private fun chooseRandomConstructor(clazz: Class<*>): Constructor<*>? {
        val randomPublicConstructor =
            try {
                clazz.declaredConstructors
                    .filter {
                        it.isPublic || !it.hasAtLeastOneOfModifiers(
                            Modifier.PROTECTED,
                            Modifier.PRIVATE
                        )
                    }
                    //Avoiding recursion
//                    .filter { it.parameterTypes.all { !it.name.contains(clazz.name) } }
                    .chooseRandomConstructor()
            } catch (_: Throwable) {
                null
            }
        val randomConstructor =
            try {
                clazz.declaredConstructors
                    .filter { it.parameterTypes.all { !it.name.contains(clazz.name) } }
                    .toList().chooseRandomConstructor()
            } catch (_: Throwable) {
                null
            }
        return if (Random.getTrue(75)) randomPublicConstructor ?: randomConstructor else randomConstructor
    }

}