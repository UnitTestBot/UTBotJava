package org.utbot.framework.context

import org.utbot.framework.context.mocker.MockerContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenerationContext
import org.utbot.framework.plugin.api.TypeReplacementMode
import org.utbot.framework.plugin.api.UtError
import soot.RefType
import soot.SootField

interface ApplicationContext : CodeGenerationContext {
    val mockerContext: MockerContext

    /**
     * Shows if there are any restrictions on type implementors.
     */
    val typeReplacementMode: TypeReplacementMode

    /**
     * Finds a type to replace the original abstract type
     * if it is guided with some additional information.
     */
    fun replaceTypeIfNeeded(type: RefType): ClassId?

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

    fun preventsFurtherTestGeneration(): Boolean

    fun getErrors(): List<UtError>
}