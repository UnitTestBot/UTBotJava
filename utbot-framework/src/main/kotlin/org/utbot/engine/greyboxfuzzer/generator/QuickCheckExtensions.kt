@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.utbot.engine.greyboxfuzzer.generator

import org.javaruntype.exceptions.TypeValidationException
import org.javaruntype.type.StandardTypeParameter
import org.javaruntype.type.Types
import org.utbot.common.withAccessibility
import org.utbot.engine.greyboxfuzzer.generator.userclasses.UserClassGenerator
import org.utbot.engine.greyboxfuzzer.util.ReflectionUtils
import org.utbot.engine.greyboxfuzzer.util.getActualTypeArguments
import org.utbot.engine.greyboxfuzzer.util.toClass
import org.utbot.engine.logger
import org.utbot.engine.rawType
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.internal.generator.ArrayGenerator
import org.utbot.quickcheck.internal.generator.GeneratorRepository
import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.ConstructorGenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsInfo
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import java.lang.reflect.*


fun Generator<*>.getAllComponents(): List<Generator<*>> {
    val queue = ArrayDeque<Generator<*>>()
    val res = mutableListOf<Generator<*>>()
    this.getComponents().forEach { queue.add(it) }
    while (queue.isNotEmpty()) {
        val comp = queue.removeFirst()
        res.add(comp)
        comp.getComponents().forEach(queue::add)
    }
    return res
}

fun Generator<*>.getComponents(): List<Generator<*>> =
    when (this) {
        is ComponentizedGenerator<*> -> this.componentGenerators()
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

fun GeneratorRepository.getOrProduceGenerator(field: Field, depth: Int = 0): Generator<*>? =
    getOrProduceGenerator(ParameterTypeContext.forField(field), depth)

fun GeneratorRepository.getOrProduceGenerator(param: Parameter, parameterIndex: Int, depth: Int = 0): Generator<*>? =
    getOrProduceGenerator(param.createParameterTypeContext(parameterIndex), depth)

fun GeneratorRepository.getOrProduceGenerator(clazz: Class<*>, depth: Int = 0): Generator<*>? =
    getOrProduceGenerator(clazz.createParameterTypeContext(), depth)

fun GeneratorRepository.getOrProduceGenerator(
    parameterTypeContext: ParameterTypeContext,
    depth: Int
): Generator<*>? {
    val producedUserClassesGenerators = mutableListOf<UserClassGenerator>()
    parameterTypeContext.getAllSubParameterTypeContexts(GreyBoxFuzzerGenerators.sourceOfRandomness).reversed()
        .forEach { typeContext ->
            try {
                this.produceGenerator(typeContext)
                //TODO catch specific exception
            } catch (e: Exception) {
                producedUserClassesGenerators += produceUserClassGenerator(typeContext.rawClass, typeContext, depth + 1)
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
        GeneratorConfigurator.configureGenerator(it, 85)
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
    val resolvedType = Types.forClass(this, *resolvedGenerics.toTypedArray())
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

//fun ParameterizedType.buildGenericsContext(): GenericsContext {
//    val clazz = this.toClass()!!
//    val klassTypeParams = clazz.typeParameters?.map { it.name }
//    val gm = LinkedHashMap<String, Type>()
//    klassTypeParams?.zip(this.actualTypeArguments)?.forEach { gm[it.first] = it.second }
//    val m = mutableMapOf(clazz to gm)
//    val genericsInfo = GenericsInfo(clazz, m)
//    return GenericsContext(genericsInfo, clazz)
//}

//fun Field.resolveFieldType(originalType: ParameterizedType): Type {
//    return originalType.buildGenericsContext().resolveFieldType(this)
//}
//
//fun Field.resolveFieldType(genericsContext: GenericsContext): Type? =
//    try {
//        genericsContext.resolveFieldType(this)
//    } catch (_: Throwable) {
//        null
//    }

//fun Field.buildParameterContext(originalType: ParameterizedType): ParameterTypeContext {
//    val ctx = originalType.buildGenericsContext()
//    return createParameterTypeContext(
//        this.name,
//        this.annotatedType,
//        this.declaringClass.name,
//        Types.forJavaLangReflectType(ctx.resolveFieldType(this)),
//        ctx
//    )
//}

//fun Field.buildParameterContext(genericsContext: GenericsContext): ParameterTypeContext {
//    return createParameterTypeContext(
//        this.name,
//        this.annotatedType,
//        this.declaringClass.name,
//        Types.forJavaLangReflectType(genericsContext.resolveFieldType(this)),
//        genericsContext
//    )
//}
//@Deprecated("Not implemented")
//fun Type.buildParameterContext(): ParameterTypeContext? {
//    val clazz = this.toClass() ?: return null
//    return if (this is ParameterizedType) {
//        buildParameterContext()
//    } else {
//        createParameterTypeContext(
//            clazz.typeName,
//            FakeAnnotatedTypeFactory.makeFrom(clazz),
//            clazz.typeName,
//            Types.forJavaLangReflectType(this),
//            GenericsResolver.resolve(clazz)
//        )
//    }
//}