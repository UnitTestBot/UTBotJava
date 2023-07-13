package org.utbot.framework.context.simple

import org.utbot.framework.UtSettings
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.isFromTrustedLibrary
import org.utbot.framework.plugin.api.ClassId
import soot.SootField

class SimpleNonNullSpeculator : NonNullSpeculator {
    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean =
        !UtSettings.maximizeCoverageUsingReflection &&
                field.declaringClass.isFromTrustedLibrary() &&
                (field.isFinal || !field.isPublic)
}