package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator
import org.utbot.engine.greyboxfuzzer.util.getTrue
import org.utbot.engine.greyboxfuzzer.util.toJavaClass
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.internal.ParameterTypeContext
import soot.Scene
import java.lang.reflect.Type
import kotlin.random.Random

class ReflectionClassGenerator(
    private val parameterTypeContext: ParameterTypeContext
) : InstanceGenerator {
    override fun generate(): UtModel {
        val packageName = parameterTypeContext.declarerName.substringBeforeLast('.')
        val randomClassFromSamePackage =
            Scene.v().classes
                .filter { it.name.startsWith(packageName) }
                .filterNot { it.isInnerClass }
                .mapNotNull { it.toJavaClass() }
                .randomOrNull()
        if (randomClassFromSamePackage != null && Random.getTrue(50)) {
            return UtModelGenerator.utModelConstructor.construct(randomClassFromSamePackage, Class::class.java.id)
        }
        Scene.v().classes.randomOrNull()?.toJavaClass()?.let {
            if (Random.getTrue(75)) {
                return UtModelGenerator.utModelConstructor.construct(it, Class::class.java.id)
            }
        }
        return UtModelGenerator.utModelConstructor.construct(Any::class.java, Class::class.java.id)
    }
}