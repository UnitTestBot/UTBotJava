package org.utbot.framework.synthesis

import org.utbot.engine.LocalVariable
import org.utbot.engine.TypeRegistry
import org.utbot.engine.selectors.strategies.ModelSynthesisScoringStrategy
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.synthesis.postcondition.constructors.ModelBasedPostConditionConstructor
import org.utbot.framework.synthesis.postcondition.constructors.PostConditionConstructor
import soot.SootClass
import soot.jimple.internal.JReturnStmt


class SynthesisUnitChecker(
    val declaringClass: SootClass,
) {
    private val synthesizer = JimpleMethodSynthesizer()
    private val typeRegistry: TypeRegistry = TypeRegistry()

    var id = 0

    fun tryGenerate(unit: SynthesisUnit, postCondition: PostConditionConstructor): UtModel? {
        if (!unit.isFullyDefined()) {
            return null
        }

        val context = synthesizer.synthesize(unit)
        val method = context.method("\$initializer_${id++}", declaringClass)

        System.err.println("Running engine...")
        val targetMap = when (postCondition) {
            is ModelBasedPostConditionConstructor -> mapOf(
                LocalVariable((context.body.units.last as JReturnStmt).op.toString()) to postCondition.expectedModel
            )
            else -> emptyMap()
        }
        val scoringStrategy = ModelSynthesisScoringStrategy(
            targetMap,
            typeRegistry
        )
        val execution = withPathSelector(PathSelectorType.SCORING_PATH_SELECTOR) {
            UtBotTestCaseGenerator.generateWithPostCondition(
                method,
                MockStrategyApi.NO_MOCKS,
                postCondition,
                scoringStrategy
            ).firstOrNull()
        } ?: return null


        return context.resolve(listOfNotNull(execution.stateBefore.thisInstance) + execution.stateBefore.parameters)
            .also {
                println("Method body:\n ${method.activeBody}")
            }
    }

    private fun <T> withPathSelector(pathSelectorType: PathSelectorType, body: () -> T): T {
        val oldSelector = UtSettings.pathSelectorType
        UtSettings.pathSelectorType = pathSelectorType
        val res = body()
        UtSettings.pathSelectorType = oldSelector
        return res
    }
}
