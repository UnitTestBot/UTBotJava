package org.utbot.usvm.jacodb

import org.usvm.instrumentation.util.jcdbSignature
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.method

val ExecutableId.jcdbSignature: String
    get() = when (this) {
        is ConstructorId -> constructor.jcdbSignature
        is MethodId -> method.jcdbSignature
    }