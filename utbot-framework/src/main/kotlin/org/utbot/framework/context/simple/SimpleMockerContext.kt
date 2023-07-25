package org.utbot.framework.context.simple

import org.utbot.framework.context.MockerContext

class SimpleMockerContext(
    override val mockFrameworkInstalled: Boolean,
    staticsMockingIsConfigured: Boolean
) : MockerContext {
    /**
     * NOTE: Can only be `true` when [mockFrameworkInstalled], because
     * situation when mock framework is not installed but static mocking
     * is configured is semantically incorrect.
     */
    override val staticsMockingIsConfigured: Boolean =
        mockFrameworkInstalled && staticsMockingIsConfigured
}