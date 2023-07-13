package org.utbot.framework.context

import org.utbot.framework.UtSettings
import org.utbot.framework.isFromTrustedLibrary
import org.utbot.framework.plugin.api.ClassId
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
class SimpleApplicationContext(
    override val mockFrameworkInstalled: Boolean = true,
    staticsMockingIsConfigured: Boolean = true,
) : ApplicationContext {
    override var staticsMockingIsConfigured = staticsMockingIsConfigured
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