@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.utbot.engine.greyboxfuzzer.generator

import org.javaruntype.exceptions.TypeValidationException
import org.javaruntype.type.StandardTypeParameter
import org.javaruntype.type.Types
import org.utbot.common.withAccessibility
import org.utbot.engine.greyboxfuzzer.generator.userclasses.UserClassGenerator
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.engine.logger
import org.utbot.engine.rawType
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.FakeAnnotatedTypeFactory
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.generator.ArrayGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.generator.GeneratorRepository
import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.ConstructorGenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsInfo
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import java.lang.reflect.*
import kotlin.random.Random


fun Generator.getAllComponents(): List<Generator> {
    val queue = ArrayDeque<Generator>()
    val res = mutableListOf<Generator>()
    this.getComponents().forEach { queue.add(it) }
    while (queue.isNotEmpty()) {
        val comp = queue.removeFirst()
        res.add(comp)
        comp.getComponents().forEach(queue::add)
    }
    return res
}

fun Generator.getComponents(): List<Generator> =
    when (this) {
        is org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator -> this.componentGenerators()
        is ArrayGenerator -> listOf(this.component)
        else -> emptyList()
    }

fun GeneratorRepository.produceUserClassGenerator(
    forClass: Class<*>,
    parameterTypeContext: ParameterTypeContext,
    depth: Int
): UserClassGenerator {
    val userClassGenerator = UserClassGenerator().also {
        it.clazz = forClass
        it.parameterTypeContext = parameterTypeContext
        it.depth = depth
    }
    addUserClassGenerator(forClass, userClassGenerator)
    return userClassGenerator
}

fun GeneratorRepository.getOrProduceGenerator(
    field: Field,
    generatorContext: GeneratorContext,
    depth: Int = 0
): Generator? =
    getOrProduceGenerator(ParameterTypeContext.forField(field), generatorContext, depth)

fun GeneratorRepository.getOrProduceGenerator(
    param: Parameter,
    parameterIndex: Int,
    generatorContext: GeneratorContext,
    depth: Int = 0
): Generator? =
    getOrProduceGenerator(param.createParameterTypeContext(parameterIndex), generatorContext, depth)

fun GeneratorRepository.getOrProduceGenerator(
    clazz: Class<*>,
    generatorContext: GeneratorContext,
    depth: Int = 0
): Generator? =
    getOrProduceGenerator(clazz.createParameterTypeContext(), generatorContext, depth)

fun GeneratorRepository.getOrProduceGenerator(
    parameterTypeContext: ParameterTypeContext,
    generatorContext: GeneratorContext,
    depth: Int
): Generator? {
    val producedUserClassesGenerators = mutableListOf<UserClassGenerator>()
    parameterTypeContext.getAllSubParameterTypeContexts(GreyBoxFuzzerGenerators.sourceOfRandomness).reversed()
        .forEach { typeContext ->
            try {
                this.produceGenerator(typeContext)
                //TODO catch specific exception
            } catch (e: Exception) {
                producedUserClassesGenerators += produceUserClassGenerator(
                    typeContext.rawClass,
                    typeContext,
                    depth + 1
                )
            }
        }
    val generator =
        try {
            this.produceGenerator(parameterTypeContext)
        } catch (e: Exception) {
            logger.debug { "Can not get generator for ${parameterTypeContext.resolved}" }
            return null
        } finally {
            producedUserClassesGenerators.forEach { removeGenerator(it.parameterTypeContext!!.resolved.rawClass) }
        }
    (listOf(generator) + generator.getAllComponents()).forEach {
        it.generatorContext = generatorContext
    }
    return generator
}

fun Parameter.createParameterTypeContext(parameterIndex: Int): ParameterTypeContext =
    try {
        //Classic scheme doesn't work for types with generics
        if (this.type.typeParameters.isNotEmpty() && this.parameterizedType !is ParameterizedType) {
            throw TypeValidationException("")
        }
        ParameterTypeContext.forParameter(this)
    } catch (e: TypeValidationException) {
        val clazz = this.type
        val parametersBounds =
            this.type.typeParameters.map { it.bounds.firstOrNull() ?: Any::class.java.rawType }.toTypedArray()
        val p = ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl(
            this.type,
            *parametersBounds
        )
        val genericContext = p.createGenericsContext(clazz)
        createParameterContextForParameter(this, parameterIndex, genericContext, p)
    }

//fun ParameterTypeContext.getAllParameterTypeContexts(): List<ParameterTypeContext> {
//    fun ArrayDeque<ParameterTypeContext>.addParameterContext(ctx: ParameterTypeContext) {
//        if (ctx.resolved.name == Zilch::class.java.name) return
//        add(ctx)
//    }
//
//    val res = mutableListOf(this)
//    val queue = ArrayDeque<ParameterTypeContext>()
//    if (this.isArray) {
//        this.arrayComponentContext().let { queue.addParameterContext(it) }
//    }
//    this.typeParameterContexts(GreyBoxFuzzerGenerators.sourceOfRandomness).forEach { queue.addParameterContext(it) }
//    while (queue.isNotEmpty()) {
//        val el = queue.removeFirst()
//        if (el.isArray) {
//            el.arrayComponentContext().let { queue.addParameterContext(it) }
//        }
//        el.typeParameterContexts(GreyBoxFuzzerGenerators.sourceOfRandomness).forEach { queue.addParameterContext(it) }
//        res.add(el)
//    }
//    return res
//}

fun Type.createGenericsContext(clazz: Class<*>): GenericsContext {
    val actualTypeParams = this.getActualTypeArguments()
    val klassTypeParams = this.toClass()?.typeParameters?.map { it.name }
    val gm = LinkedHashMap<String, Type>()
    klassTypeParams?.zip(actualTypeParams)?.forEach { gm[it.first] = it.second }
    val m = mutableMapOf(clazz to gm)
    val genericsInfo = GenericsInfo(clazz, m)
    return GenericsContext(genericsInfo, clazz)
}

fun Class<*>.createParameterTypeContext(): ParameterTypeContext {
    val generics = GenericsResolver.resolve(this)
    val resolvedGenerics =
        generics.resolveTypeGenerics(this).map { createStandardTypeParameter(Types.forJavaLangReflectType(it)) }
    val resolvedType = try {
        Types.forClass(this, *resolvedGenerics.toTypedArray())
    } catch (e: Throwable) {
        Types.forClass(this)
    }
    return ParameterTypeContext(
        this.typeName,
        FakeAnnotatedTypeFactory.makeFrom(this),
        this.typeName,
        resolvedType,
        generics
    )
}

fun createParameterContextForParameter(
    parameter: Parameter,
    parameterIndex: Int,
    generics: GenericsContext
): ParameterTypeContext {
    val exec = parameter.declaringExecutable
    val clazz = exec.declaringClass
    val declarerName = clazz.name
    val resolvedType =
        when (generics) {
            is MethodGenericsContext -> generics.resolveParameterType(parameterIndex)
            is ConstructorGenericsContext -> generics.resolveParameterType(parameterIndex)
            else -> throw IllegalArgumentException("Unexpected type of GenericsContext")
        }
    return ParameterTypeContext(
        parameter.name,
        parameter.annotatedType,
        declarerName,
        Types.forJavaLangReflectType(
            resolvedType
        ),
        generics,
        parameterIndex
    )
}

fun createParameterContextForParameter(
    parameter: Parameter,
    parameterIndex: Int,
    generics: GenericsContext,
    type: Type
): ParameterTypeContext {
    val exec = parameter.declaringExecutable
    val clazz = exec.declaringClass
    val declarerName = clazz.name + '.' + exec.name
    return ParameterTypeContext(
        parameter.name,
        parameter.annotatedType,
        declarerName,
        ReflectionUtils.forJavaReflectTypeSafe(type),
        generics,
        parameterIndex
    )
}

private fun createStandardTypeParameter(type: org.javaruntype.type.Type<*>): StandardTypeParameter<*> {
    val constructor = StandardTypeParameter::class.java.declaredConstructors.first()
    constructor.isAccessible = true
    return constructor.withAccessibility {
        constructor.newInstance(type) as StandardTypeParameter<*>
    }
}

object QuickCheckExtensions {

    fun getRandomImplementerGenericContext(clazz: Class<*>, resolvedType: Type): GenericsContext? {
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
        var prevImplementer = clazz
        resolvedType.getActualTypeArguments().forEachIndexed { index, typeVariable ->
            if (prevImplementer.toClass() != null) {
                generics.add(typeVariable to mutableListOf(prevImplementer.toClass()!!.typeParameters[index]))
            }
        }
        for (implementer in randomImplementersChain) {
            val javaImplementer = implementer.toJavaClass() ?: return null
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
        val g =
            prevImplementer.typeParameters.associate { tp -> tp.name to generics.find { it.second.last() == tp }?.first }
        val gm = LinkedHashMap<String, Type>()
        g.forEach {
            if (it.value != null) {
                gm[it.key] = it.value!!
            }
        }
        val m = mutableMapOf(prevImplementer to gm)
        return GenericsContext(GenericsInfo(prevImplementer, m), prevImplementer)
    }

    fun buildGenericsContextForInterfaceParent(
        resolvedType: Type,
        clazz: Class<*>,
        parentChain: List<Class<*>>
    ): GenericsContext? {
        val generics = mutableListOf<Pair<Type, MutableList<Type>>>()
        var curClass = clazz
        resolvedType.getActualTypeArguments().forEachIndexed { index, typeVariable ->
            if (curClass.toClass() != null) {
                generics.add(typeVariable to mutableListOf(curClass.toClass()!!.typeParameters[index]))
            }
        }
        for (parent in parentChain) {
            val parentType = curClass.let { it.genericInterfaces.toList() + listOf(it.genericSuperclass) }
                .find { it.toClass() == parent }
            val tp = curClass.typeParameters
            curClass = parent
            if (tp.isEmpty()) continue
            val newTp = parentType?.getActualTypeArguments()?.ifEmpty { return null } ?: return null
            tp.mapIndexed { index, typeVariable -> typeVariable to newTp[index] }
                .forEach { typeVar ->
                    val indexOfTypeParam = generics.indexOfFirst { it.second.last() == typeVar.first }
                    if (indexOfTypeParam != -1) {
                        generics[indexOfTypeParam].second.add(curClass.typeParameters[indexOfTypeParam])
                    }
                }
        }
        val g = curClass.typeParameters.associate { tp -> tp.name to generics.find { it.second.last() == tp }?.first }
        val gm = LinkedHashMap<String, Type>()
        g.forEach {
            if (it.value != null) {
                gm[it.key] = it.value!!
            }
        }
        val m = mutableMapOf(curClass to gm)
        return GenericsContext(GenericsInfo(curClass, m), curClass)
    }
}