package org.utbot.framework.context

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import soot.SootField

/**
 * Checks whether accessing [field] (with a method invocation or field access) speculatively
 * cannot produce [NullPointerException] (according to its finality or accessibility).
 *
 * @see docs/SpeculativeFieldNonNullability.md for more information.
 *
 * NOTE: methods for both [FieldId] and [SootField] are provided, because for some lambdas
 * class names in byte code and in Soot do not match, making conversion between two field
 * representations not always possible, which in turn makes us to support both [FieldId]
 * and [SootField] to be useful for both fuzzer and symbolic engine respectively.
 */
interface NonNullSpeculator {
    fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean

    fun speculativelyCannotProduceNullPointerException(
        field: FieldId,
        classUnderTest: ClassId,
    ): Boolean
}
