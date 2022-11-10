package org.utbot.framework.codegen.model.util

import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgVariable

data class CgExceptionHandler(
        val exception: CgVariable,
        val statements: List<CgStatement>
)

class CgExceptionHandlerBuilder {
    lateinit var exception: CgVariable
    lateinit var statements: List<CgStatement>

    fun build(): CgExceptionHandler {
        return CgExceptionHandler(exception, statements)
    }
}

internal fun buildExceptionHandler(init: CgExceptionHandlerBuilder.() -> Unit): CgExceptionHandler {
    return CgExceptionHandlerBuilder().apply(init).build()
}
