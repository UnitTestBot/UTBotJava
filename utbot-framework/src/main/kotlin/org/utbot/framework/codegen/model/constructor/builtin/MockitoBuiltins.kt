package org.utbot.framework.codegen.model.constructor.builtin

import org.utbot.framework.plugin.api.builtInClass
import org.utbot.framework.plugin.api.util.*
import org.utbot.jcdb.api.MethodId

internal val mockitoBuiltins: Set<MethodId>
    get() = setOf(
        mockMethodId, whenMethodId, thenMethodId, thenReturnMethodId,
        any, anyOfClass, anyByte, anyChar, anyShort, anyInt, anyLong,
        anyFloat, anyDouble, anyBoolean, anyString
    )

internal val mockitoClassId get() = builtInClass(name = "org.mockito.Mockito")

internal val ongoingStubbingClassId get() = builtInClass(name = "org.mockito.stubbing.OngoingStubbing")

internal val answerClassId get() = builtInClass(name = "org.mockito.stubbing.Answer")

internal val argumentMatchersClassId get() = builtInClass(name = "org.mockito.ArgumentMatchers")

internal val mockedConstructionContextClassId get() = builtInClass(name = "org.mockito.MockedConstruction.Context", isNested = true)

internal val mockMethodId get() = mockitoClassId.newBuiltinStaticMethodId(
    name = "mock",
    // actually this is an unbounded type parameter
    returnType = objectClassId,
    arguments = listOf(Class::class.id)
)

internal val whenMethodId get() = mockitoClassId.newBuiltinStaticMethodId(
    name = "when",
    returnType = ongoingStubbingClassId,
    // argument type is actually an unbounded type parameter
    arguments = listOf(objectClassId)
)

@Suppress("unused")
internal val thenMethodId get() = ongoingStubbingClassId.newBuiltinMethod(
    name = "then",
    returnType = ongoingStubbingClassId,
    arguments = listOf(answerClassId)
)

internal val thenReturnMethodId get() = ongoingStubbingClassId.newBuiltinMethod(
    name = "thenReturn",
    returnType = ongoingStubbingClassId,
    arguments = listOf(objectClassId)
)

// TODO: for this method and other static methods implement some utils that allow calling
// TODO: these methods without explicit declaring class id specification, because right now such calls are too verbose
internal val any get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "any",
    // return type is actually an unbounded type parameter
    returnType = objectClassId
)

internal val anyOfClass get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "any",
    returnType = objectClassId,
    arguments = listOf(Class::class.id)
)

internal val anyByte get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyByte",
    returnType = byteClassId
)

internal val anyChar get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyChar",
    returnType = charClassId
)

internal val anyShort get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyShort",
    returnType = shortClassId
)

internal val anyInt get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyInt",
    returnType = intClassId
)

internal val anyLong get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyLong",
    returnType = longClassId
)

internal val anyFloat get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyFloat",
    returnType = floatClassId
)

internal val anyDouble get() =argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyDouble",
    returnType = doubleClassId
)

internal val anyBoolean get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyBoolean",
    returnType = booleanClassId
)

@Suppress("unused")
internal val anyString get() = argumentMatchersClassId.newBuiltinStaticMethodId(
    name = "anyString",
    returnType = stringClassId
)