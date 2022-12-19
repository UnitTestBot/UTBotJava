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
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime
import java.util.IdentityHashMap

private val logger = KotlinLogging.logger {}

typealias JavaValueProvider = ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>

class FuzzedDescription(
    val description: FuzzedMethodDescription,
    val tracer: Trie<Instruction, *>,
    val typeCache: IdentityHashMap<Type, FuzzedType>,
) : Description<FuzzedType>(
    description.parameters.mapIndexed { index, classId ->
        description.fuzzerType(index) ?: FuzzedType(classId)
    }
) {
    val constants: Sequence<FuzzedConcreteValue>
        get() = description.concreteValues.asSequence()
}

fun defaultValueProviders(idGenerator: IdentityPreservingIdGenerator<Int>) = listOf(
    BooleanValueProvider,
    IntegerValueProvider,
    FloatValueProvider,
    StringValueProvider,
    NumberValueProvider,
    ObjectValueProvider(idGenerator),
    ArrayValueProvider(idGenerator),
    EnumValueProvider(idGenerator),
    ListSetValueProvider(idGenerator),
    MapValueProvider(idGenerator),
    IteratorValueProvider(idGenerator),
    EmptyCollectionValueProvider(idGenerator),
    DateValueProvider(idGenerator),
//    NullValueProvider,
)

suspend fun runJavaFuzzing(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    methodUnderTest: ExecutableId,
    constants: Collection<FuzzedConcreteValue>,
    names: List<String>,
    providers: List<ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>> = defaultValueProviders(idGenerator),
    exec: suspend (thisInstance: FuzzedValue?, description: FuzzedDescription, values: List<FuzzedValue>) -> BaseFeedback<Trie.Node<Instruction>, FuzzedType, FuzzedValue>
) {
    val classUnderTest = methodUnderTest.classId
    val name = methodUnderTest.classId.simpleName + "." + methodUnderTest.name
    val returnType = methodUnderTest.returnType
    val parameters = methodUnderTest.parameters

    // For a concrete fuzzing run we need to track types we create.
    // Because of generics can be declared as recursive structures like `<T extends Iterable<T>>`,
    // we should track them by reference and do not call `equals` and `hashCode` recursively.
    val typeCache = IdentityHashMap<Type, FuzzedType>()
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
    val descriptionWithOptionalThisInstance = FuzzedDescription(createFuzzedMethodDescription(thisInstance), tracer, typeCache)
    val descriptionWithOnlyParameters = FuzzedDescription(createFuzzedMethodDescription(null), tracer, typeCache)
    try {
        logger.info { "Starting fuzzing for method: $methodUnderTest" }
        logger.info { "\tuse thisInstance = ${thisInstance != null}" }
        logger.info { "\tparameters = $parameters" }
        var totalExecutionCalled = 0
        val totalFuzzingTime = measureNanoTime {
            runFuzzing(ValueProvider.of(providers), descriptionWithOptionalThisInstance) { _, t ->
                totalExecutionCalled++
                if (thisInstance == null) {
                    exec(null, descriptionWithOnlyParameters, t)
                } else {
                    exec(t.first(), descriptionWithOnlyParameters, t.drop(1))
                }
            }
        }
        logger.info { "Finishing fuzzing for method: $methodUnderTest in ${TimeUnit.NANOSECONDS.toMillis(totalFuzzingTime)} ms" }
        logger.info { "\tTotal execution called: $totalExecutionCalled" }
    } catch (t: Throwable) {
        logger.info(t) { "Fuzzing is stopped because of an error" }
    }
}

/**
 * Resolve a fuzzer type that has class info and some generics.
 */
internal fun toFuzzerType(type: Type, cache: IdentityHashMap<Type, FuzzedType>): FuzzedType {
    return toFuzzerType(
        type = type,
        classId = { t -> toClassId(t, cache) },
        generics = { t -> toGenerics(t) },
        cache = cache
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
    cache: IdentityHashMap<Type, FuzzedType>
): FuzzedType {
    val g = mutableListOf<FuzzedType>()
    var target = cache[type]
    if (target == null) {
        target = FuzzedType(classId(type), g)
        cache[type] = target
        g += generics(type).map {
            toFuzzerType(it, classId, generics, cache)
        }
    }
    return target
}

private fun toClassId(type: Type, cache: IdentityHashMap<Type, FuzzedType>): ClassId {
    return when (type) {
        is WildcardType -> type.upperBounds.firstOrNull()?.let { toClassId(it, cache) } ?: objectClassId
        is TypeVariable<*> -> type.bounds.firstOrNull()?.let { toClassId(it, cache) } ?: objectClassId
        is GenericArrayType -> {
            val genericComponentType = type.genericComponentType
            val classId = toClassId(genericComponentType, cache)
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
        is TypeVariable<*> -> arrayOf(t.bounds.firstOrNull() ?: java.lang.Object::class.java)
        is GenericArrayType -> toGenerics(t.genericComponentType)
        is ParameterizedType -> t.actualTypeArguments
        is Class<*> -> t.typeParameters
        else -> emptyArray()
    }
}