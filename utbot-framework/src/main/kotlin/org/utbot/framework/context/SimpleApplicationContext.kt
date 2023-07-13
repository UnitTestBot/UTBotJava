package org.utbot.framework.context

import org.utbot.framework.UtSettings
import org.utbot.framework.context.mocker.MockerContext
import org.utbot.framework.isFromTrustedLibrary
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeReplacementMode
import org.utbot.framework.plugin.api.UtError
import soot.RefType
import soot.SootField

/**
 * A context to use when no specific data is required.
 */
class SimpleApplicationContext(
    override val mockerContext: MockerContext
) : ApplicationContext {
    override val typeReplacementMode: TypeReplacementMode = TypeReplacementMode.AnyImplementor

    override fun replaceTypeIfNeeded(type: RefType): ClassId? = null

    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean =
        !UtSettings.maximizeCoverageUsingReflection &&
                field.declaringClass.isFromTrustedLibrary() &&
                (field.isFinal || !field.isPublic)

    override fun preventsFurtherTestGeneration(): Boolean = false

    override fun getErrors(): List<UtError> = emptyList()
}