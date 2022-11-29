@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.common.isAbstract
import org.utbot.engine.greyboxfuzzer.generator.DataGenerator
import org.utbot.engine.greyboxfuzzer.generator.QuickCheckExtensions
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.engine.logger
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Type
import kotlin.random.Random

class InterfaceImplementationsInstanceGenerator(
    private val resolvedType: Type,
    private val typeContext: GenericsContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val generatorContext: GeneratorContext,
    private val depth: Int
) : InstanceGenerator {
    override fun generate(): UtModel {
        //Try to generate with statics with some probability
        val clazz = resolvedType.toClass() ?: return UtNullModel(objectClassId)
        if (Random.getTrue(50)) {
            try {
                StaticsBasedInstanceGenerator(
                    clazz,
                    typeContext,
                    sourceOfRandomness,
                    generationStatus,
                    generatorContext,
                    depth
                ).generate().let {
                    if (it !is UtNullModel) return it
                }
            } catch (e: Throwable) {
                logger.error { "Something wrong during StaticsBasedInstanceGenerator work" }
            }
        }
        val genericsContext =
            QuickCheckExtensions.getRandomImplementerGenericContext(clazz, resolvedType) ?: return generateMock(
                clazz,
                resolvedType,
                typeContext,
                generatorContext
            )
        return ClassesInstanceGenerator(
            genericsContext.currentClass(),
            genericsContext,
            null,
            GenerationMethod.ANY,
            sourceOfRandomness,
            generationStatus,
            generatorContext,
            depth
        ).generate().let {
            if (it is UtNullModel && Random.getTrue(50)) generateMock(clazz, resolvedType, typeContext, generatorContext) else it
        }
    }

    private fun generateMock(clazz: Class<*>, resolvedType: Type, typeContext: GenericsContext, generatorContext: GeneratorContext,): UtModel {
        if (!clazz.isInterface) return UtNullModel(clazz.id)
        val sootClazz = clazz.toSootClass() ?: return UtNullModel(clazz.id)
        val constructor = generatorContext.utModelConstructor
        val allNeededInterfaces = clazz.methods.map { it.declaringClass }.filter { it != clazz }.toSet()
        val chainToGenericsContext = allNeededInterfaces.map { cl ->
            val chain = cl.toSootClass()
                ?.getImplementersOfWithChain()
                ?.filter { it.contains(sootClazz) }
                ?.map { it.dropLastWhile { it != sootClazz } }
                ?.minByOrNull { it.size }
                ?.map { it.toJavaClass() }
            if (chain == null || chain.any { it == null }) {
                null
            } else {
                cl to QuickCheckExtensions.buildGenericsContextForInterfaceParent(
                    resolvedType,
                    clazz,
                    chain.map { it!! }.reversed().drop(1)
                )
            }
        }
        val allChainToGenericsContext = chainToGenericsContext + (clazz to typeContext)
        val mocks = clazz.methods
            .filter { it.isAbstract }
            .associateTo(mutableMapOf()) { method ->
                val genericsContextForMethod =
                    allChainToGenericsContext.find { it!!.first == method.declaringClass }?.second
                val methodReturnType =
                    if (genericsContextForMethod != null) {
                        genericsContextForMethod.method(method).resolveReturnType().let {
                            if (it.toClass() == null) method.returnType else it
                        }
                    } else {
                        method.returnType
                    }
                val parameterTypeContext = ParameterTypeContext.forType(methodReturnType, genericsContextForMethod)
                val generatedUtModelWithReturnType =
                    try {
                        DataGenerator.generateUtModel(parameterTypeContext, depth, generatorContext, sourceOfRandomness, generationStatus)
                    } catch (_: Throwable) {
                        UtNullModel(methodReturnType.toClass()!!.id)
                    }
                method.executableId as ExecutableId to listOf(generatedUtModelWithReturnType)
            }
        return UtCompositeModel(constructor.computeUnusedIdAndUpdate(), clazz.id, isMock = true, mocks = mocks)
    }

//    private fun buildGenericsContextForInterfaceParent(resolvedType: Type, clazz: Class<*>, parentChain: List<Class<*>>): GenericsContext? {
//        val generics = mutableListOf<Pair<Type, MutableList<Type>>>()
//        var curClass = clazz
//        resolvedType.getActualTypeArguments().forEachIndexed { index, typeVariable ->
//            if (curClass.toClass() != null) {
//                generics.add(typeVariable to mutableListOf(curClass.toClass()!!.typeParameters[index]))
//            }
//        }
//        for (parent in parentChain) {
//            val parentType = curClass.let { it.genericInterfaces.toList() + listOf(it.genericSuperclass) }
//                .find { it.toClass() == parent }
//            val tp = curClass.typeParameters
//            curClass = parent
//            if (tp.isEmpty()) continue
//            val newTp = parentType?.getActualTypeArguments()?.ifEmpty { return null } ?: return null
//            tp.mapIndexed { index, typeVariable -> typeVariable to newTp[index] }
//                .forEach { typeVar ->
//                    val indexOfTypeParam = generics.indexOfFirst { it.second.last() == typeVar.first }
//                    if (indexOfTypeParam != -1) {
//                        generics[indexOfTypeParam].second.add(curClass.typeParameters[indexOfTypeParam])
//                    }
//                }
//        }
//        val g = curClass.typeParameters.associate { tp -> tp.name to generics.find { it.second.last() == tp }?.first }
//        val gm = LinkedHashMap<String, Type>()
//        g.forEach {
//            if (it.value != null) {
//                gm[it.key] = it.value!!
//            }
//        }
//        val m = mutableMapOf(curClass to gm)
//        return GenericsContext(GenericsInfo(curClass, m), curClass)
//    }
}