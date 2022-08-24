package org.utbot.framework.synthesis

import mu.KotlinLogging
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.synthesis.postcondition.constructors.ConstraintBasedPostConditionConstructor
import soot.SootClass

class SynthesisUnitChecker(
    val testCaseGenerator: TestCaseGenerator,
    val declaringClass: SootClass,
) {
    private val logger = KotlinLogging.logger("ConstrainedSynthesisUnitChecker")

    var id = 0

    fun tryGenerate(synthesisUnitContext: SynthesisUnitContext, parameters: List<UtModel>): List<UtModel>? {
        if (!synthesisUnitContext.isFullyDefined) return null

        val synthesisMethodContext = SynthesisMethodContext(synthesisUnitContext)
        val method = synthesisMethodContext.method("\$initializer_${id++}", declaringClass)

        val execution = run {
            val executions = testCaseGenerator.generateWithPostCondition(
                method,
                MockStrategyApi.NO_MOCKS,
                ConstraintBasedPostConditionConstructor(
                    parameters,
                    synthesisUnitContext,
                    synthesisMethodContext
                )
            )
            executions.firstOrNull { it.result is UtExecutionSuccess }
        } ?: return null


        logger.error { execution }
        return synthesisMethodContext.resolve(listOfNotNull(execution.stateBefore.thisInstance) + execution.stateBefore.parameters)
    }

    private fun <T> withPathSelector(pathSelectorType: PathSelectorType, body: () -> T): T {
        val oldSelector = UtSettings.pathSelectorType
        UtSettings.pathSelectorType = pathSelectorType
        val res = body()
        UtSettings.pathSelectorType = oldSelector
        return res
    }
}
