package org.utbot.framework.concrete

/**
 * Some information, which is computed after classes instrumentation.
 *
 * This information will be used later in `invoke` function.
 */
class InstrumentationContext {
    /**
     * Contains unique id for each method, which is required for this method mocking.
     */
    val methodSignatureToId = mutableMapOf<String, Int>()
}
