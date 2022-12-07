package org.utbot.fuzzing

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.*
import org.utbot.fuzzing.providers.*
import org.utbot.fuzzing.utils.Trie
import java.lang.reflect.*

typealias JavaValueProvider = ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>

class FuzzedDescription(
    val description: FuzzedMethodDescription,
    val tracer: Trie<Instruction, *>,
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
    EmptyCollectionValueProvider(idGenerator),
    DateValueProvider(idGenerator),
//    NullValueProvider,
)

suspend fun runJavaFuzzing(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    methodUnderTest: ExecutableId,
    constants: Collection<FuzzedConcreteValue>,
    names: List<String>,
    mock: (ClassId) -> Boolean = { false },
    providers: List<ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>> = defaultValueProviders(idGenerator),
    exec: suspend (thisInstance: FuzzedValue?, description: FuzzedDescription, values: List<FuzzedValue>) -> BaseFeedback<Trie.Node<Instruction>, FuzzedType, FuzzedValue>
) {
    val classUnderTest = methodUnderTest.classId
    val name = methodUnderTest.classId.simpleName + "." + methodUnderTest.name
    val returnType = methodUnderTest.returnType
    val parameters = methodUnderTest.parameters

    /**
     * To fuzz this instance the class of it is added into head of parameters list.
     * Done for compatibility with old fuzzer logic and should be reworked more robust way.
     */
    fun createFuzzedMethodDescription(self: ClassId?) = FuzzedMethodDescription(
        name, returnType, listOfNotNull(self) + parameters, constants
    ).apply {
        compilableName = if (!methodUnderTest.isConstructor) methodUnderTest.name else null
        className = classUnderTest.simpleName
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
                    self != null && it == 0 -> toFuzzerType(methodUnderTest.executable.declaringClass)
                    self != null -> toFuzzerType(methodUnderTest.executable.genericParameterTypes[it - 1])
                    else -> toFuzzerType(methodUnderTest.executable.genericParameterTypes[it])
                }
            } catch (_: Throwable) {
                null
            }
        }
        shouldMock = mock
    }

    val thisInstance = with(methodUnderTest) {
        if (!isStatic && !isConstructor) { classUnderTest } else { null }
    }
    val tracer = Trie(Instruction::id)
    val descriptionWithOptionalThisInstance = FuzzedDescription(createFuzzedMethodDescription(thisInstance), tracer)
    val descriptionWithOnlyParameters = FuzzedDescription(createFuzzedMethodDescription(null), tracer)
    BaseFuzzing(providers) { _, t ->
        if (thisInstance == null) {
            exec(null, descriptionWithOnlyParameters, t)
        } else {
            exec(t.first(), descriptionWithOnlyParameters, t.drop(1))
        }
    }.fuzz(descriptionWithOptionalThisInstance)
}

private fun toFuzzerType(type: Type): FuzzedType {
    return when (type) {
        is WildcardType -> type.upperBounds.firstOrNull()?.let(::toFuzzerType) ?: FuzzedType(objectClassId)
        is TypeVariable<*> -> type.bounds.firstOrNull()?.let(::toFuzzerType) ?: FuzzedType(objectClassId)
        is ParameterizedType -> FuzzedType((type.rawType as Class<*>).id, type.actualTypeArguments.map { toFuzzerType(it) })
        is GenericArrayType -> {
            val genericComponentType = type.genericComponentType
            val fuzzerType = toFuzzerType(genericComponentType)
            val classId = if (genericComponentType !is GenericArrayType) {
                ClassId("[L${fuzzerType.classId.name};", fuzzerType.classId)
            } else {
                ClassId("[" + fuzzerType.classId.name, fuzzerType.classId)
            }
            FuzzedType(classId)
        }
        is Class<*> -> FuzzedType(type.id, type.typeParameters.map { toFuzzerType(it) })
        else -> error("Unknown type: $type")
    }
}