package org.utbot.framework.context

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeReplacementMode
import soot.RefType

interface TypeReplacer {
    /**
     * Shows if there are any restrictions on type implementors.
     */
    val typeReplacementMode: TypeReplacementMode

    /**
     * Finds a type to replace the original abstract type
     * if it is guided with some additional information.
     */
    fun replaceTypeIfNeeded(type: RefType): ClassId?
}