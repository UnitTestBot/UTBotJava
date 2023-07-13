package org.utbot.framework.context

interface MockerContext {
    /**
     * Shows if we have installed framework dependencies
     */
    val mockFrameworkInstalled: Boolean

    /**
     * Shows if we have installed static mocking tools
     */
    val staticsMockingIsConfigured: Boolean
}