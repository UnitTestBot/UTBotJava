package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.util.SootStaticsCollector
import org.utbot.engine.greyboxfuzzer.util.hasModifiers
import org.utbot.engine.greyboxfuzzer.util.toClass
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Method
import java.lang.reflect.Modifier

//TODO filter not suitable methods with generics with bad bounds
//TODO make it work for subtypes
internal class StaticsMethodBasedInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val generatorContext: GeneratorContext,
    private val depth: Int
) : InstanceGenerator {
    override fun generate(): UtModel =
        getRandomStaticToProduceInstanceUsingSoot()?.let { methodToProvideInstance ->
            val resolvedMethodContext =
                try {
                    gctx.method(methodToProvideInstance)
                } catch (_: Throwable) {
                    null
                }
            ExecutableInvoker(
                methodToProvideInstance,
                clazz,
                methodToProvideInstance.executableId,
                resolvedMethodContext,
                sourceOfRandomness,
                generationStatus,
                generatorContext,
                depth
            ).invoke()
        } ?: UtNullModel(clazz.id)

    //In case of no Soot
    private fun getRandomStaticToProduceInstance(): Method? =
        try {
            clazz.declaredMethods.filter { it.hasModifiers(Modifier.STATIC) }
                .map { it to gctx.method(it).resolveReturnType() }
                .filter { it.first.returnType.toClass() == clazz }
                .filter { it.first.parameterTypes.all { !it.name.contains(clazz.name) } }
                .randomOrNull()?.first
        } catch (e: Error) {
            null
        }

    private fun getRandomStaticToProduceInstanceUsingSoot(): Method? =
        SootStaticsCollector.getStaticMethodsInitializersOf(clazz).randomOrNull()
}