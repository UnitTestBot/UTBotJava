package org.utbot.framework.context

import org.utbot.framework.UtSettings
import org.utbot.framework.isFromTrustedLibrary
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenerationContext
import org.utbot.framework.plugin.api.TypeReplacementMode
import org.utbot.framework.plugin.api.UtError
import soot.RefType
import soot.SootField

/**
 * A context to use when no specific data is required.
 *
 * @param mockFrameworkInstalled shows if we have installed framework dependencies
 * @param staticsMockingIsConfigured shows if we have installed static mocking tools
 */
open class ApplicationContext(
    val mockFrameworkInstalled: Boolean = true,
    staticsMockingIsConfigured: Boolean = true,
) : CodeGenerationContext {
    var staticsMockingIsConfigured = staticsMockingIsConfigured
        private set

    init {
        /**
         * Situation when mock framework is not installed but static mocking is configured is semantically incorrect.
         *
         * However, it may be obtained in real application after this actions:
         * - fully configure mocking (dependency installed + resource file created)
         * - remove mockito-core dependency from project
         * - forget to remove mock-maker file from resource directory
         *
         * Here we transform this configuration to semantically correct.
         */
        if (!mockFrameworkInstalled && staticsMockingIsConfigured) {
            this.staticsMockingIsConfigured = false
        }
    }

    /**
     * Shows if there are any restrictions on type implementors.
     */
    open val typeReplacementMode: TypeReplacementMode = TypeReplacementMode.AnyImplementor

    /**
     * Finds a type to replace the original abstract type
     * if it is guided with some additional information.
     */
    open fun replaceTypeIfNeeded(type: RefType): ClassId? = null

    /**
     * Sets the restrictions on speculative not null
     * constraints in current application context.
     *
     * @see docs/SpeculativeFieldNonNullability.md for more information.
     */
    open fun avoidSpeculativeNotNullChecks(field: SootField): Boolean =
        UtSettings.maximizeCoverageUsingReflection || !field.declaringClass.isFromTrustedLibrary()

    /**
     * Checks whether accessing [field] (with a method invocation or field access) speculatively
     * cannot produce [NullPointerException] (according to its finality or accessibility).
     *
     * @see docs/SpeculativeFieldNonNullability.md for more information.
     */
    open fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean = field.isFinal || !field.isPublic

    open fun preventsFurtherTestGeneration(): Boolean = false

    open fun getErrors(): List<UtError> = emptyList()
}