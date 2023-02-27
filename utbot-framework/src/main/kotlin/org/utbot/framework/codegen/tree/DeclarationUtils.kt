package org.utbot.framework.codegen.tree

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel

/**
 * Checks if an expected variable is needed,
 * or it will be further asserted with assertNull or assertTrue/False.
 */
fun needExpectedDeclaration(model: UtModel): Boolean {
    val representsNull = model is UtNullModel
    val representsBoolean = model is UtPrimitiveModel && model.value is Boolean
    return !(representsNull || representsBoolean)
}

/**
 * Contains all possible visibility modifiers that may be used in code generation.
 */
enum class VisibilityModifier {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    INTERNAL,
    PACKAGEPRIVATE,
}