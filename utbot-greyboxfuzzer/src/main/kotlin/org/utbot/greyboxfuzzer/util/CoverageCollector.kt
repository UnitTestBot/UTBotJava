package org.utbot.greyboxfuzzer.util

import org.utbot.framework.plugin.api.Instruction
import java.util.concurrent.ConcurrentHashMap

object CoverageCollector {

    private object Covered
    private val coveredInstructions = ConcurrentHashMap<Instruction, Covered>()
    val coverage: Set<Instruction>
        get() = coveredInstructions.keys

    fun addCoverage(coverage: Set<Instruction>) {
        this.coveredInstructions.putAll(coverage.map { it to Covered })
    }

    fun addCoverage(instruction: Instruction) {
        this.coveredInstructions[instruction] = Covered
    }

    fun clear() {
        coveredInstructions.clear()
    }
}