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

internal val mockMethodId get() = builtinStaticMethodId(
    classId = mockitoClassId,
    name = "mock",
    // actually this is an unbounded type parameter
    returnType = objectClassId,
    arguments = arrayOf(Class::class.id)
)

internal val whenMethodId get() = builtinStaticMethodId(
    classId = mockitoClassId,
    name = "when",
    returnType = ongoingStubbingClassId,
    // argument type is actually an unbounded type parameter
    arguments = arrayOf(objectClassId)
)

@Suppress("unused")
internal val thenMethodId get() = builtinMethodId(
    classId = ongoingStubbingClassId,
    name = "then",
    returnType = ongoingStubbingClassId,
    arguments = arrayOf(answerClassId)
)

internal val thenReturnMethodId get() = builtinMethodId(
    classId = ongoingStubbingClassId,
    name = "thenReturn",
    returnType = ongoingStubbingClassId,
    arguments = arrayOf(objectClassId)
)

// TODO: for this method and other static methods implement some utils that allow calling
// TODO: these methods without explicit declaring class id specification, because right now such calls are too verbose
internal val any get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "any",
    // return type is actually an unbounded type parameter
    returnType = objectClassId
)

internal val anyOfClass get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "any",
    returnType = objectClassId,
    arguments = arrayOf(Class::class.id)
)

internal val anyByte get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyByte",
    returnType = byteClassId
)

internal val anyChar get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyChar",
    returnType = charClassId
)

internal val anyShort get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyShort",
    returnType = shortClassId
)

internal val anyInt get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyInt",
    returnType = intClassId
)

internal val anyLong get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyLong",
    returnType = longClassId
)

internal val anyFloat get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyFloat",
    returnType = floatClassId
)

internal val anyDouble get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyDouble",
    returnType = doubleClassId
)

internal val anyBoolean get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyBoolean",
    returnType = booleanClassId
)

@Suppress("unused")
internal val anyString get() = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyString",
    returnType = stringClassId
)