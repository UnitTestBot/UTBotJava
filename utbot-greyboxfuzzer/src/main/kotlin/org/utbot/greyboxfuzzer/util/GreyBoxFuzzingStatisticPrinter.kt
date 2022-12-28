package org.utbot.greyboxfuzzer.util

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution

object GreyBoxFuzzingStatisticPrinter {

    fun printFuzzingStats(methods2executions: Map<ExecutableId, MutableList<UtExecution>>) {
        val methodsToInstructionsNumbers = methods2executions.entries.map { (method, executions) ->
            val methodInstructions =
                (executions.firstOrNull() as? UtGreyBoxFuzzedExecution)?.fuzzingResult?.methodInstructions ?: setOf()
            method to methodInstructions
        }
        logger.debug { "OVERALL RESULTS:" }
        logger.debug { "------------------------------------------" }
        for ((method, instructions) in methodsToInstructionsNumbers) {
            val coveredMethodInstructions = CoverageCollector.coverage
                .asSequence()
                .filter { it.className == method.classId.name.replace('.', '/') }
                .filter { it.methodSignature == method.signature }
                .toSet()
            val coveredMethodLines = coveredMethodInstructions.map { it.lineNumber }.toSet()
            val methodLines = instructions.map { it.lineNumber }.toSet()

            logger.debug { "METHOD: ${method.name}" }
            logger.debug { "COVERED INSTRUCTIONS: ${coveredMethodInstructions.size} from ${instructions.size} ${coveredMethodInstructions.size.toDouble() / instructions.size * 100}%" }
            logger.debug { "COVERED INSTRUCTIONS: ${coveredMethodInstructions.map { it.id }.sorted()}" }
            logger.debug { "NOT COVERED INSTRUCTIONS: ${instructions.filter { it !in coveredMethodInstructions }.map { it.id }.sorted()}" }
            logger.debug { "------" }
            logger.debug { "COVERED LINES: ${coveredMethodLines.size} from ${methodLines.size} ${coveredMethodLines.size.toDouble() / methodLines.size * 100}%" }
            logger.debug { "COVERED LINES: ${coveredMethodLines.sorted()}" }
            logger.debug { "NOT COVERED LINES: ${methodLines.filter { it !in coveredMethodLines }.sorted()}" }
            logger.debug { "----------------------" }
        }
        logger.debug { "-----------------------------------------------------" }
        val allInstructionsToCover = methodsToInstructionsNumbers.flatMap { it.second }.toSet()
        val allClassNames = methodsToInstructionsNumbers.map { it.first.classId }.toSet().map { it.name }
        val allInstructionsToCoverSize = allInstructionsToCover.size
        val allMethodLineNumbersSize = methodsToInstructionsNumbers.flatMap { it.second.map { it.lineNumber }.toSet() }.size
        val coveredInstructions =
            CoverageCollector.coverage
                .filter { it.className.replace('/', '.') in allClassNames }
                .toSet()
                .filter { it in allInstructionsToCover }
        val numberOfCoveredLines = coveredInstructions.map { it.lineNumber }.toSet().size
        val numberOfCoveredInstructions = coveredInstructions.size
        logger.debug { "IN INSTRUCTIONS FINALLY COVERED $numberOfCoveredInstructions from $allInstructionsToCoverSize ${numberOfCoveredInstructions.toDouble() / allInstructionsToCoverSize * 100}%" }
        logger.debug { "IN LINES FINALLY COVERED $numberOfCoveredLines from $allMethodLineNumbersSize ${numberOfCoveredLines.toDouble() / allMethodLineNumbersSize * 100}%" }
        logger.debug { "------------------------------------------" }
    }
}