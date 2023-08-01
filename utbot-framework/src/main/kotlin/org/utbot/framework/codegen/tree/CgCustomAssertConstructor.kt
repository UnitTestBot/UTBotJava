package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.UtCustomModel

interface CgCustomAssertConstructor {
    val context: CgContext
    fun tryConstructCustomAssert(expected: UtCustomModel, actual: CgVariable): Boolean
}

class CgSimpleCustomAssertConstructor(
    override val context: CgContext
) : CgCustomAssertConstructor,
    CgContextOwner by context {
    override fun tryConstructCustomAssert(expected: UtCustomModel, actual: CgVariable): Boolean =
        false
}
