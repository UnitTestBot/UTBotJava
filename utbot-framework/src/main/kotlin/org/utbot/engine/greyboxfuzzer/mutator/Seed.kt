package org.utbot.engine.greyboxfuzzer.mutator

import org.utbot.engine.greyboxfuzzer.generator.FParameter
import org.utbot.engine.greyboxfuzzer.generator.ThisInstance
import org.utbot.framework.plugin.api.EnvironmentModels

data class Seed(
    val thisInstance: ThisInstance,
    val parameters: List<FParameter>,
    var score: Double = 0.0
) {

    fun createEnvironmentModels(): EnvironmentModels {
        return EnvironmentModels(thisInstance.utModelForExecution, parameters.map { it.utModel }, mapOf())
    }

    fun copy(): Seed {
        return Seed(thisInstance.copy(), parameters.map { it.copy() }, score)
    }

    fun replaceFParameter(index: Int, newFParameter: FParameter): Seed {
        return Seed(
            thisInstance.copy(),
            parameters.mapIndexed { ind, fParameter -> if (ind == index) newFParameter else fParameter.copy() },
            score
        )
    }
}