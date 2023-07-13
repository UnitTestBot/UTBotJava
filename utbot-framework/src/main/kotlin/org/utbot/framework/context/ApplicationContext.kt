package org.utbot.framework.context

import org.utbot.framework.plugin.api.CodeGenerationContext
import org.utbot.framework.plugin.api.UtError

interface ApplicationContext : CodeGenerationContext {
    val mockerContext: MockerContext
    val typeReplacer: TypeReplacer
    val nonNullSpeculator: NonNullSpeculator

    fun preventsFurtherTestGeneration(): Boolean

    fun getErrors(): List<UtError>
}