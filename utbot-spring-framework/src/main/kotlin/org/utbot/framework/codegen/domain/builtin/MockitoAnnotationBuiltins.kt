package org.utbot.framework.codegen.domain.builtin

import org.utbot.framework.plugin.api.BuiltinClassId

internal val mockClassId = BuiltinClassId(
    canonicalName = "org.mockito.Mock",
    simpleName = "Mock",
)

internal val spyClassId = BuiltinClassId(
    canonicalName = "org.mockito.Spy",
    simpleName = "Spy"
)

internal val injectMocksClassId = BuiltinClassId(
    canonicalName = "org.mockito.InjectMocks",
    simpleName = "InjectMocks",
)