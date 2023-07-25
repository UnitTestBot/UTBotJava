package org.utbot.framework.context

import org.utbot.framework.plugin.api.ClassId
import soot.SootField

interface NonNullSpeculator {
    /**
     * Checks whether accessing [field] (with a method invocation or field access) speculatively
     * cannot produce [NullPointerException] (according to its finality or accessibility).
     *
     * @see docs/SpeculativeFieldNonNullability.md for more information.
     */
    fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean
}
