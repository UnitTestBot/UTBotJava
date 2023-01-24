package org.utbot.greyboxfuzzer.generator.userclasses.generator

import org.utbot.greyboxfuzzer.util.getTrue
import org.utbot.greyboxfuzzer.util.toJavaClass
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.util.getAllTypesFromCastAndInstanceOfInstructions
import org.utbot.greyboxfuzzer.util.toSootMethod
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import soot.Scene
import kotlin.random.Random

class ReflectionClassGenerator(
    private val parameterTypeContext: ParameterTypeContext,
    private val generatorContext: GeneratorContext
) : InstanceGenerator {
    override fun generate(): UtModel {
        val packageName = parameterTypeContext.declarerName.substringBeforeLast('.')
        val currentSootMethod =
            (parameterTypeContext.generics as? MethodGenericsContext)?.currentMethod()?.toSootMethod()
        val potentialUsefulClasses =
            currentSootMethod?.getAllTypesFromCastAndInstanceOfInstructions() ?: setOf()
        val randomClassToGenerate =
            if (potentialUsefulClasses.isNotEmpty() && Random.getTrue(50)) {
                potentialUsefulClasses.random()
            } else {
                Scene.v().classes
                    .filter { it.name.startsWith(packageName) }
                    .filterNot { it.isInnerClass }
                    .mapNotNull { it.toJavaClass() }
                    .randomOrNull()
            }
        if (randomClassToGenerate != null) {
            return generatorContext.utModelConstructor.construct(randomClassToGenerate, Class::class.java.id)
        }
//        Scene.v().classes.randomOrNull()?.toJavaClass()?.let {
//            if (Random.getTrue(75)) {
//                return generatorContext.utModelConstructor.construct(it, Class::class.java.id)
//            }
//        }
        return generatorContext.utModelConstructor.construct(Any::class.java, Class::class.java.id)
    }
}