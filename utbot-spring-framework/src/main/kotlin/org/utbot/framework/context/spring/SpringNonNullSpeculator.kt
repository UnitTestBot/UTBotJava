package org.utbot.framework.context.spring

import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.fieldId
import soot.SootField

class SpringNonNullSpeculator(
    private val delegateNonNullSpeculator: NonNullSpeculator,
    private val springApplicationContext: SpringApplicationContext
) : NonNullSpeculator {
    override fun speculativelyCannotProduceNullPointerException(field: SootField, classUnderTest: ClassId): Boolean =
        // TODO add ` || delegateNonNullSpeculator.speculativelyCannotProduceNullPointerException(field, classUnderTest)`
        //  (TODO is added as a part of only equivalent transformations refactoring PR and should be completed in the follow up PR)
        field.fieldId in classUnderTest.allDeclaredFieldIds && field.type.classId !in springApplicationContext.allInjectedSuperTypes

}