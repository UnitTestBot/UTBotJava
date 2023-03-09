package org.utbot.taint

import org.utbot.taint.model.TaintConfiguration

/**
 * Mutable state that is modified during the taint analyzer work.
 */
class TaintContext(
    val markManager: TaintMarkManager,
    val configuration: TaintConfiguration,
)
