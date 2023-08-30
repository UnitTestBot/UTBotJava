package org.utbot.modifications.util

import org.utbot.framework.plugin.api.BuiltinClassId

val kotlinIntrinsicsClassId: BuiltinClassId
    get() = BuiltinClassId(
        simpleName = "Intrinsics",
        canonicalName = "kotlin.jvm.internal.Intrinsics",
        packageName = "kotlin.jvm.internal"
    )