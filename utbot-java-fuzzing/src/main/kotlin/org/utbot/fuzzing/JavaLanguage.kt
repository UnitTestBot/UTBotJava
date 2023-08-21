package org.utbot.fuzzing

import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.*
import org.utbot.fuzzing.providers.*
import org.utbot.fuzzing.utils.Trie
import java.lang.reflect.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

typealias JavaValueProvider = ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>



class FuzzedDescription(
    val description: FuzzedMethodDescription,
    val tracer: Trie<Instruction, *>,
    val typeCache: MutableMap<Type, FuzzedType>,
    val random: Random,
    val scope: Scope? = null,
) : Description<FuzzedType, FuzzedValue>(
    description.parameters.mapIndexed { index, classId ->
        description.fuzzerType(index) ?: FuzzedType(classId)
    }
) {
    // To turn on logging, use this implementation of Description:
    //LoggingDescription<FuzzedType, FuzzedValue>(
    // description.parameters.mapIndexed { index, classId ->
    //    description.fuzzerType(index) ?: FuzzedType(classId)
    //}, "~/.utbot/JavaFuzzing") { ... }

    val constants: Sequence<FuzzedConcreteValue>
        get() = description.concreteValues.asSequence()

    override fun clone(scope: Scope): FuzzedDescription {
        return FuzzedDescription(description, tracer, typeCache, random, scope)
    }
}


fun defaultValueProviders(idGenerator: IdentityPreservingIdGenerator<Int>) = listOf(
    BooleanValueProvider,
    IntegerValueProvider,
    FloatValueProvider,
    StringValueProvider,
    NumberValueProvider,
    anyObjectValueProvider(idGenerator),
    ArrayValueProvider(idGenerator),
    EnumValueProvider(idGenerator),
    ListSetValueProvider(idGenerator),
    MapValueProvider(idGenerator),
    IteratorValueProvider(idGenerator),
    EmptyCollectionValueProvider(idGenerator),
    DateValueProvider(idGenerator),
    NullValueProvider,
)


data class JavaFeedback(
    val result: Trie.Node<Instruction>,
    override val control: Control,
    override val weight: Int = result.getTraceLen(),
    ) : WeightedFeedback<FuzzedType, FuzzedValue> {
    override fun toString(): String {
        return "$result; trace hash: ${result.hashCode()}; trace count: ${result.count}"
    }
}


suspend fun runJavaFuzzing(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    methodUnderTest: ExecutableId,
    constants: Collection<FuzzedConcreteValue>,
    names: List<String>,
    providers: List<ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>> = defaultValueProviders(idGenerator),
    exec: suspend (thisInstance: FuzzedValue?, description: FuzzedDescription, values: List<FuzzedValue>) -> JavaFeedback
) {
    val random = Random(0)
    val classUnderTest = methodUnderTest.classId
    val name = methodUnderTest.classId.simpleName + "." + methodUnderTest.name
    val returnType = methodUnderTest.returnType
    val parameters = methodUnderTest.parameters

    // For a concrete fuzzing run we need to track types we create.
    // Because of generics can be declared as recursive structures like `<T extends Iterable<T>>`,
    // we should track them by reference and do not call `equals` and `hashCode` recursively.
    val typeCache = hashMapOf<Type, FuzzedType>()
    /**
     * To fuzz this instance, the class of it is added into head of parameters list.
     * Done for compatibility with old fuzzer logic and should be reworked more robust way.
     */
    fun createFuzzedMethodDescription(self: ClassId?) = FuzzedMethodDescription(
        name, returnType, listOfNotNull(self) + parameters, constants
    ).apply {
        compilableName = if (!methodUnderTest.isConstructor) methodUnderTest.name else null
        className = classUnderTest.simpleName
        canonicalName = classUnderTest.canonicalName
        isNested = classUnderTest.isNested
        isStatic = methodUnderTest.isStatic
        packageName = classUnderTest.packageName
        parameterNameMap = { index ->
            when {
                self != null && index == 0 -> "this"
                self != null -> names.getOrNull(index - 1)
                else -> names.getOrNull(index)
            }
        }
        fuzzerType = {
            try {
                when {
                    self != null && it == 0 -> toFuzzerType(methodUnderTest.executable.declaringClass, typeCache)
                    self != null -> toFuzzerType(methodUnderTest.executable.genericParameterTypes[it - 1], typeCache)
                    else -> toFuzzerType(methodUnderTest.executable.genericParameterTypes[it], typeCache)
                }
            } catch (_: Throwable) {
                null
            }
        }
        shouldMock = { false }
    }

    val thisInstance = with(methodUnderTest) {
        if (!isStatic && !isConstructor) { classUnderTest } else { null }
    }
    val tracer = Trie(Instruction::id)
    val descriptionWithOptionalThisInstance = FuzzedDescription(createFuzzedMethodDescription(thisInstance), tracer, typeCache, random)
    val descriptionWithOnlyParameters = FuzzedDescription(createFuzzedMethodDescription(null), tracer, typeCache, random)
    val start = System.nanoTime()
    try {
        logger.info { "Starting fuzzing for method: $methodUnderTest" }
        logger.info { "\tuse thisInstance = ${thisInstance != null}" }
        logger.info { "\tparameters = $parameters" }
        var totalExecutionCalled = 0
        runFuzzing(
            provider = ValueProvider.of(providers),
            description = descriptionWithOptionalThisInstance, random,
            configuration = Configuration()
        ) { _, t ->
            totalExecutionCalled++
            if (thisInstance == null) {
                exec(null, descriptionWithOnlyParameters, t)
            } else {
                exec(t.first(), descriptionWithOnlyParameters, t.drop(1))
            }
        }
        val totalFuzzingTime = System.nanoTime() - start
        logger.info { "Finishing fuzzing for method: $methodUnderTest in ${TimeUnit.NANOSECONDS.toMillis(totalFuzzingTime)} ms" }
        logger.info { "\tTotal execution called: $totalExecutionCalled" }
    } catch (ce: CancellationException) {
        val totalFuzzingTime = System.nanoTime() - start
        logger.info { "Fuzzing is stopped because of timeout. Total execution time: ${TimeUnit.NANOSECONDS.toMillis(totalFuzzingTime)} ms" }
        logger.debug(ce) { "\tStacktrace:" }
    } catch (t: Throwable) {
        logger.info(t) { "Fuzzing is stopped because of an error" }
    }
}

/**
 * Traverse though type hierarchy of this fuzzed type.
 * Ignores all set [FuzzedType.generics] of source type.
 *
 * todo Process types like `Fuzzed[Any, generics = T1, T2]` to match those T1 and T2 types with superclass and interfaces
 */
internal fun FuzzedType.traverseHierarchy(typeCache: MutableMap<Type, FuzzedType>): Sequence<FuzzedType> = sequence {
    val typeQueue = mutableListOf(this@traverseHierarchy)
    var index = 0
    while (typeQueue.isNotEmpty()) {
        val next = typeQueue.removeFirst()
        if (index++ > 0) {
            yield(next)
        }
        val jClass = next.classId.jClass
        val superclass = jClass.genericSuperclass
        if (superclass != null) {
            typeQueue += toFuzzerType(superclass, typeCache)
        }
        typeQueue += jClass.genericInterfaces.asSequence().map { toFuzzerType(it, typeCache) }
    }
}

/**
 * Resolve a fuzzer type that has class info and some generics.
 *
 * @param type to be resolved
 * @param cache is used to store same [FuzzedType] for same java types
 */
internal fun toFuzzerType(type: Type, cache: MutableMap<Type, FuzzedType>): FuzzedType {
    return toFuzzerType(
        type = type,
        classId = { t -> toClassId(t, cache) },
        generics = { t -> toGenerics(t) },
        cache = cache,
    )
}

/**
 * Resolve a fuzzer type that has class info and some generics.
 *
 * Cache is used to stop recursive call in case of some recursive class definition like:
 *
 * ```
 * public <T extends Iterable<T>> call(T type) { ... }
 * ```
 *
 * @param type to be resolved into a fuzzed type.
 * @param classId is a function that produces classId by general type.
 * @param generics is a function that produced a list of generics for this concrete type.
 * @param cache is used to store all generated types.
 */
private fun toFuzzerType(
    type: Type,
    classId: (type: Type) -> ClassId,
    generics: (parent: Type) -> Array<out Type>,
    cache: MutableMap<Type, FuzzedType>,
): FuzzedType {
    val g = mutableListOf<FuzzedType>()
    val t = type.replaceWithUpperBoundUntilNotTypeVariable()
    var target = cache[t]
    if (target == null) {
        target = FuzzedType(classId(t), g)
        cache[t] = target
        g += generics(t).map {
            toFuzzerType(it, classId, generics, cache)
        }
    }
    return target
}

internal fun Type.replaceWithUpperBoundUntilNotTypeVariable() : Type {
    var type: Type = this
    while (type is TypeVariable<*>) {
        type = type.bounds.firstOrNull() ?: java.lang.Object::class.java
    }
    return type
}

private fun toClassId(type: Type, cache: MutableMap<Type, FuzzedType>): ClassId {
    return when (type) {
        is WildcardType -> type.upperBounds.firstOrNull()?.let { toClassId(it, cache) } ?: objectClassId
        is GenericArrayType -> {
            val genericComponentType = type.genericComponentType
            val classId = toFuzzerType(genericComponentType, cache).classId
            if (genericComponentType !is GenericArrayType) {
                ClassId("[L${classId.name};", classId)
            } else {
                ClassId("[" + classId.name, classId)
            }
        }
        is ParameterizedType -> (type.rawType as Class<*>).id
        is Class<*> -> type.id
        else -> error("unknown type: $type")
    }
}

private fun toGenerics(t: Type) : Array<out Type> {
    return when (t) {
        is WildcardType -> t.upperBounds.firstOrNull()?.let { toGenerics(it) } ?: emptyArray()
        is GenericArrayType -> arrayOf(t.genericComponentType)
        is ParameterizedType -> t.actualTypeArguments
        is Class<*> -> t.typeParameters
        else -> emptyArray()
    }
}