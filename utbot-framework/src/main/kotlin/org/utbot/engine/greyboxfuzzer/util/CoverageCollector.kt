package org.utbot.engine.greyboxfuzzer.util

import org.utbot.framework.plugin.api.Instruction
import java.util.concurrent.CopyOnWriteArraySet

object CoverageCollector {

    val coverage = CopyOnWriteArraySet<Instruction>()
}