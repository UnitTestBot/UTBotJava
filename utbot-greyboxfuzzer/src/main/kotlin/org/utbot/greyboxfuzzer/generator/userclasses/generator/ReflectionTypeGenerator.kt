package org.utbot.greyboxfuzzer.generator.userclasses.generator

import org.utbot.greyboxfuzzer.util.getTrue
import org.utbot.greyboxfuzzer.util.toJavaClass
import org.utbot.greyboxfuzzer.util.toSootMethod
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.greyboxfuzzer.generator.GreyBoxFuzzerGeneratorsAndSettings
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.util.getAllTypesFromCastAndInstanceOfInstructions
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import soot.NullType
import soot.Scene
import soot.jimple.Stmt
import java.lang.reflect.Type
import kotlin.random.Random

class ReflectionTypeGenerator(
    private val parameterTypeContext: ParameterTypeContext,
    private val generatorContext: GeneratorContext
    ) : InstanceGenerator {

    override fun generate(): UtModel {
        val currentSootMethod =
            (parameterTypeContext.generics as? MethodGenericsContext)?.currentMethod()?.toSootMethod()
        val potentialUsefulClasses =
            currentSootMethod?.getAllTypesFromCastAndInstanceOfInstructions() ?: setOf()
        val randomTypeToGenerate =
            if (Random.getTrue(50) && potentialUsefulClasses.isNotEmpty()) {
                potentialUsefulClasses.random()
            } else {
                currentSootMethod?.activeBody?.units?.filterIsInstance<Stmt>()
                    ?.flatMap { it.useBoxes.map { it.value.type } }
                    ?.filter { it !is NullType }
                    ?.toSet()
                    ?.mapNotNull {
                        try {
                            it.classId.jClass
                        } catch (e: Throwable) {
                            null
                        }
                    }?.randomOrNull()
            }
        if (Random.getTrue(50) && randomTypeToGenerate != null) {
            return generatorContext.utModelConstructor.construct(randomTypeToGenerate, Type::class.java.id)
        }

        val packageName = parameterTypeContext.declarerName.substringBeforeLast('.')
        val randomClassFromSamePackage =
            Scene.v().classes
                .filter { it.name.startsWith(packageName) }
                .filterNot { it.isInnerClass }
                .mapNotNull { it.toJavaClass() }
                .randomOrNull()
        if (randomClassFromSamePackage != null && Random.getTrue(25)) {
            return generatorContext.utModelConstructor.construct(randomClassFromSamePackage, Type::class.java.id)
        }
        return GreyBoxFuzzerGeneratorsAndSettings.generatorRepository
            .getGenerators()
            .toList()
            .random()
            .let {
                generatorContext.utModelConstructor.construct(it.first, Type::class.java.id)
            }
//        try {
//            Scene.v().classes.randomOrNull()?.toJavaClass()?.let {
//                return generatorContext.utModelConstructor.construct(it, Type::class.java.id)
//            }
//        } catch (e: Throwable) {
//            return generatorContext.utModelConstructor.construct(Any::class.java, Type::class.java.id)
//        }
        return generatorContext.utModelConstructor.construct(Any::class.java, Type::class.java.id)
    }
}