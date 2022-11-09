@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsInfo
import java.lang.reflect.Type
import kotlin.random.Random

class InterfaceImplementationsInstanceGenerator(
    private val resolvedType: Type,
    private val typeContext: GenericsContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val depth: Int
) : InstanceGenerator {
    override fun generate(): UtModel? {
        //Try to generate with statics with some probability
        val clazz = resolvedType.toClass() ?: return null
        if (Random.getTrue(50)) {
            StaticsBasedInstanceGenerator(
                clazz,
                typeContext,
                sourceOfRandomness,
                generationStatus,
                depth
            ).generate()?.let {
                return it
            }
        }
        val genericsContext = getRandomImplementerGenericContext(clazz, resolvedType) ?: return null
        return ClassesInstanceGenerator(
            genericsContext.currentClass(),
            genericsContext,
            null,
            GenerationMethod.ANY,
            sourceOfRandomness,
            generationStatus,
            depth
        ).generate()
    }

    private fun getRandomImplementerGenericContext(clazz: Class<*>, resolvedType: Type): GenericsContext? {
        val sootClass = clazz.toSootClass() ?: return null
        val implementers =
            sootClass.getImplementersOfWithChain()
                .filter { it.all { !it.toString().contains("$") } }
                .filter { it.last().isConcrete }
        val randomImplementersChain =
            if (Random.getTrue(75)) {
                implementers.shuffled().minByOrNull { it.size }?.drop(1)
            } else {
                implementers.randomOrNull()?.drop(1)
            } ?: return null
        //Deal with generics
        val generics = mutableListOf<Pair<Type, MutableList<Type>>>()
        var prevImplementer = sootClass.toJavaClass()
        resolvedType.getActualTypeArguments().forEachIndexed { index, typeVariable ->
            if (prevImplementer.toClass() != null) {
                generics.add(typeVariable to mutableListOf(prevImplementer.toClass()!!.typeParameters[index]))
            }
        }
        for (implementer in randomImplementersChain) {
            val javaImplementer = implementer.toJavaClass()
            val extendType = javaImplementer.let { it.genericInterfaces + it.genericSuperclass }
                .find { it.toClass() == prevImplementer }
            val tp = prevImplementer.typeParameters
            prevImplementer = javaImplementer
            if (tp.isEmpty()) continue
            val newTp = extendType?.getActualTypeArguments()?.ifEmpty { return null } ?: return null
            tp.mapIndexed { index, typeVariable -> typeVariable to newTp[index] }
                .forEach { typeVar ->
                    val indexOfTypeParam = generics.indexOfFirst { it.second.last() == typeVar.first }
                    if (indexOfTypeParam != -1) {
                        generics[indexOfTypeParam].second.add(typeVar.second)
                    }
                }
        }
        val g = prevImplementer.typeParameters.map { tp -> tp.name to generics.find { it.second.last() == tp }?.first }.toMap()
        val gm = LinkedHashMap<String, Type>()
        g.forEach { gm[it.key] = it.value!! }
        val m = mutableMapOf(prevImplementer to gm)
        return GenericsContext(GenericsInfo(prevImplementer, m), prevImplementer)
    }
}