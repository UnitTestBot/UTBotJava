package org.utbot.framework.synthesis

import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.synthesis.postcondition.constructors.PostConditionConstructor
import soot.SootClass


class SynthesisUnitChecker(
    val declaringClass: SootClass,
) {
    private val synthesizer = JimpleMethodSynthesizer()

    var id = 0

    fun tryGenerate(unit: SynthesisUnit, postCondition: PostConditionConstructor): UtModel? {
        if (!unit.isFullyDefined()) {
            return null
        }

        val context = synthesizer.synthesize(unit)
        val method = context.method("\$initializer_${id++}", declaringClass)

        System.err.println("Running engine...")
        val execution = UtBotTestCaseGenerator.generateWithPostCondition(
            method,
            MockStrategyApi.NO_MOCKS,
            postCondition
        ).firstOrNull() ?: return null


        return context.resolve(listOfNotNull(execution.stateBefore.thisInstance) + execution.stateBefore.parameters).also {
            println("Method body:\n ${method.activeBody}")
        }
    }
}
