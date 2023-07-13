package org.utbot.framework.context

import org.utbot.framework.plugin.api.UtError

interface ConcreteExecutionContext {
    fun preventsFurtherTestGeneration(): Boolean

    fun getErrors(): List<UtError>

    // TODO refactor, so this interface only includes the following:
    //  val instrumentation: Instrumentation<UtConcreteExecutionResult>
    //  fun createValueProviderOrThrow(classUnderTest: ClassId, idGenerator: IdentityPreservingIdGenerator<Int>): JavaValueProvider
    //  fun loadContext(): ContextLoadingResult
    //  fun Coverage.filterCoveredInstructions(classUnderTestId: ClassId): Coverage
}