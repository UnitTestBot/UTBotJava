package org.utbot.engine.greyboxfuzzer.util

import org.utbot.engine.logger
import org.utbot.framework.concrete.UtFuzzingConcreteExecutionResult
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.signature
import org.utbot.framework.util.sootMethod
import soot.Scene
import kotlin.io.path.appendText
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

object GreyBoxFuzzingStatisticPrinter {

    fun printFuzzingStats(methods2executions: Map<ExecutableId, MutableList<UtExecution>>) {
//        //Printing to console
        val clazz = methods2executions.keys.first().classId
        val methodsToInstructionsNumbers = methods2executions.entries.map { (method, executions) ->
            val methodInstructions =
                (executions.firstOrNull() as? UtGreyBoxFuzzedExecution)?.fuzzingResult?.methodInstructionsIds ?: setOf()
            method to methodInstructions
        }
        logger.debug { "OVERALL RESULTS:" }
        logger.debug { "------------------------------------------" }
        for ((method, instructions) in methodsToInstructionsNumbers) {
            val coveredMethodInstructions = CoverageCollector.coverage
                .filter { it.methodSignature == method.signature }
                .map { it.id }
                .toSet()
                .filter { it in instructions }

            logger.debug { "METHOD: ${method.name}" }
            logger.debug { "COVERED: ${coveredMethodInstructions.size} from ${instructions.size} ${coveredMethodInstructions.size.toDouble() / instructions.size * 100}%" }
            logger.debug { "COVERED: ${coveredMethodInstructions.sorted()}" }
            logger.debug { "NOT COVERED: ${instructions.filter { it !in coveredMethodInstructions }.sorted()}" }
            logger.debug { "------------------" }
        }
        logger.debug { "------------------------------------------" }
        val allInstructionsToCover = methodsToInstructionsNumbers.flatMap { it.second }.toSet()
        val allInstructionsToCoverSize = allInstructionsToCover.size
        val allCoveredLines = CoverageCollector.coverage
            .filter { it.className.replace('/', '.') == clazz.name }
            .map { it.id }.toSet()
            .filter { it in allInstructionsToCover }
            .size
        logger.debug { "FINALLY COVERED $allCoveredLines from $allInstructionsToCoverSize ${allCoveredLines.toDouble() / allInstructionsToCoverSize * 100}%" }
        logger.debug { "------------------------------------------" }
    }
}