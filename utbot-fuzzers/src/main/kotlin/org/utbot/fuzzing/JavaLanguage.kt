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
    val genThisInstance: ClassId?,
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
    exec: suspend (description: FuzzedDescription, values: List<FuzzedValue>) -> BaseFeedback<Trie.Node<Instruction>, FuzzedType, FuzzedValue>
) {
    val classUnderTest = methodUnderTest.classId
    val thisInstance = with(methodUnderTest) {
        if (!isStatic && !isConstructor) {
            classUnderTest
        } else {
            null
        }
    }
    val name = methodUnderTest.classId.simpleName + "." + methodUnderTest.name
    val returnType = methodUnderTest.returnType
    val parameters = listOfNotNull(thisInstance) + methodUnderTest.parameters

    val fmd = FuzzedMethodDescription(
        name, returnType, parameters, constants
    ).apply {
        compilableName = if (!methodUnderTest.isConstructor) methodUnderTest.name else null
        className = classUnderTest.simpleName
        packageName = classUnderTest.packageName
        parameterNameMap = { index -> names.getOrNull(index) }
        fuzzerType = {
            try {
                when {
                    thisInstance != null && it == 0 -> toFuzzerType(classUnderTest.jClass)
                    thisInstance != null -> toFuzzerType(methodUnderTest.executable.genericParameterTypes[it - 1])
                    else -> toFuzzerType(methodUnderTest.executable.genericParameterTypes[it])
                }
            } catch (_: Throwable) {
                null
            }
        }
        shouldMock = mock
    }
    BaseFuzzing(providers, exec)
        .fuzz(FuzzedDescription(fmd, Trie(Instruction::id), thisInstance))
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
        is Class<*> -> FuzzedType(type.id)
        else -> error("Unknown type: $type")
    }
}