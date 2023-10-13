package org.utbot.framework.context.simple

import org.utbot.framework.UtSettings
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.isFromTrustedLibrary
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.isFinal
import org.utbot.framework.plugin.api.util.isPublic
import soot.SootField

class SimpleNonNullSpeculator : NonNullSpeculator {
    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean =
        !UtSettings.maximizeCoverageUsingReflection &&
                field.declaringClass.isFromTrustedLibrary() &&
                (field.isFinal || !field.isPublic)

    override fun speculativelyCannotProduceNullPointerException(
        field: FieldId,
        classUnderTest: ClassId
    ): Boolean =
        !UtSettings.maximizeCoverageUsingReflection &&
                field.declaringClass.isFromTrustedLibrary() &&
                (field.isFinal || !field.isPublic)
}