package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.fieldId
import soot.SootField

class SpringNonNullSpeculator(
    private val delegateNonNullSpeculator: NonNullSpeculator,
    private val springApplicationContext: SpringApplicationContext
) : NonNullSpeculator {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val loggedSpeculations = mutableSetOf<Speculation>()
    }

    private data class Speculation(
        val field: FieldId,
        val isMocked: Boolean,
        val classUnderTest: ClassId,
        val speculativelyCannotProduceNPE: Boolean,
    )

    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        isMocked: Boolean,
        classUnderTest: ClassId
    ): Boolean {
        if (delegateNonNullSpeculator.speculativelyCannotProduceNullPointerException(field, isMocked, classUnderTest))
            return true

        if (field.fieldId !in classUnderTest.allDeclaredFieldIds)
            return false

        val speculativelyCannotProduceNPE = isMocked

        val speculation = Speculation(field.fieldId, isMocked, classUnderTest, speculativelyCannotProduceNPE)
        if (loggedSpeculations.add(speculation))
            logger.info { "New speculation: $speculation" }

        return speculativelyCannotProduceNPE
    }
}