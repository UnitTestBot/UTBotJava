package org.utbot.framework.codegen.domain.builtin

import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.builtinMethodId
import org.utbot.framework.plugin.api.util.builtinStaticMethodId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId

internal val mockitoClassId = BuiltinClassId(
    canonicalName = "org.mockito.Mockito",
    simpleName = "Mockito",
)

internal val mockClassId = BuiltinClassId(
    canonicalName = "org.mockito.Mock",
    simpleName = "Mock",
)

internal val injectMocksClassId = BuiltinClassId(
    canonicalName = "org.mockito.InjectMocks",
    simpleName = "InjectMocks",
)

internal val ongoingStubbingClassId = BuiltinClassId(
    canonicalName = "org.mockito.stubbing.OngoingStubbing",
    simpleName = "OngoingStubbing",
)

internal val answerClassId = BuiltinClassId(
    canonicalName = "org.mockito.stubbing.Answer",
    simpleName = "Answer",
)

internal val argumentMatchersClassId = BuiltinClassId(
    canonicalName = "org.mockito.ArgumentMatchers",
    simpleName = "ArgumentMatchers",
)

internal val mockedConstructionContextClassId = BuiltinClassId(
    canonicalName = "org.mockito.MockedConstruction.Context",
    simpleName = "Context",
    name = "org.mockito.MockedConstruction\$Context",
    isNested = true,
)

internal val mockMethodId = builtinStaticMethodId(
    classId = mockitoClassId,
    name = "mock",
    // actually this is an unbounded type parameter
    returnType = objectClassId,
    arguments = arrayOf(Class::class.id)
)

internal val whenMethodId = builtinStaticMethodId(
    classId = mockitoClassId,
    name = "when",
    returnType = ongoingStubbingClassId,
    // argument type is actually an unbounded type parameter
    arguments = arrayOf(objectClassId)
)

@Suppress("unused")
internal val thenMethodId = builtinMethodId(
    classId = ongoingStubbingClassId,
    name = "then",
    returnType = ongoingStubbingClassId,
    arguments = arrayOf(answerClassId)
)

internal val thenReturnMethodId = builtinMethodId(
    classId = ongoingStubbingClassId,
    name = "thenReturn",
    returnType = ongoingStubbingClassId,
    arguments = arrayOf(objectClassId)
)

// TODO: for this method and other static methods implement some utils that allow calling
// TODO: these methods without explicit declaring class id specification, because right now such calls are too verbose
internal val any = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "any",
    // return type is actually an unbounded type parameter
    returnType = objectClassId
)

internal val anyOfClass = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "any",
    returnType = objectClassId,
    arguments = arrayOf(Class::class.id)
)

internal val anyByte = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyByte",
    returnType = byteClassId
)

internal val anyChar = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyChar",
    returnType = charClassId
)

internal val anyShort = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyShort",
    returnType = shortClassId
)

internal val anyInt = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyInt",
    returnType = intClassId
)

internal val anyLong = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyLong",
    returnType = longClassId
)

internal val anyFloat = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyFloat",
    returnType = floatClassId
)

internal val anyDouble = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyDouble",
    returnType = doubleClassId
)

internal val anyBoolean = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyBoolean",
    returnType = booleanClassId
)

@Suppress("unused")
internal val anyString = builtinStaticMethodId(
    classId = argumentMatchersClassId,
    name = "anyString",
    returnType = stringClassId
)

