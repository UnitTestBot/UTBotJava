package org.utbot.engine.greyboxfuzzer.util

import org.utbot.engine.logger
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.signature
import soot.Scene
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

object GreyBoxFuzzingStatisticPrinter {

    fun printFuzzingStats(methods: List<ExecutableId>) {
//        //Printing to console
        val clazz = methods.first().classId
        val sootClazz = Scene.v().classes.find { it.name == clazz.name }!!
        val methodsToLineNumbers = sootClazz.methods.mapNotNull { sootMethod ->
            val javaMethod = sootMethod.toJavaMethod()
            if (javaMethod?.kotlinFunction != null) {
                javaMethod to sootMethod.activeBody.units
                    .map { it.javaSourceStartLineNumber }
                    .filter { it != -1 }
                    .toSet()
            } else {
                null
            }
        }
        logger.debug { "OVERALL RESULTS:" }
        logger.debug { "------------------------------------------" }
        for ((method, lines) in methodsToLineNumbers) {
            val coveredMethodInstructions = CoverageCollector.coverage
                .filter { it.methodSignature == method.signature }
                .map { it.lineNumber }
                .toSet()
                .filter { it in lines }

            logger.debug { "METHOD: ${method.name}" }
            logger.debug { "COVERED: ${coveredMethodInstructions.size} from ${lines.size} ${coveredMethodInstructions.size.toDouble() / lines.size * 100}%" }
            logger.debug { "COVERED: ${coveredMethodInstructions.sorted()}" }
            logger.debug { "NOT COVERED: ${lines.filter { it !in coveredMethodInstructions }.sorted()}" }
            logger.debug { "------------------" }
        }
        logger.debug { "------------------------------------------" }
        val allLinesToCover = methodsToLineNumbers.flatMap { it.second }.toSet()
        val allLinesToCoverSize = allLinesToCover.size
        val allCoveredLines = CoverageCollector.coverage
            .filter { it.className.replace('/', '.') == clazz.name }
            .map { it.lineNumber }.toSet()
            .filter { it in allLinesToCover }
            .size
        logger.debug { "FINALLY COVERED $allCoveredLines from $allLinesToCoverSize ${allCoveredLines.toDouble() / allLinesToCoverSize * 100}%" }
        logger.debug { "------------------------------------------" }
    }
}