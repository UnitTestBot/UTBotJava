package org.utbot.greyboxfuzzer.mutator

import org.utbot.greyboxfuzzer.generator.FParameter
import org.utbot.greyboxfuzzer.generator.ThisInstance
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.Instruction

data class Seed(
    val thisInstance: ThisInstance,
    val parameters: List<FParameter>,
    val instructionCoverage: Set<Instruction>,
    var score: Double = 0.0
) {

    fun createEnvironmentModels(): EnvironmentModels {
        return EnvironmentModels(thisInstance.utModelForExecution, parameters.map { it.utModel }, mapOf())
    }

    fun copy(): Seed {
        return Seed(thisInstance.copy(), parameters.map { it.copy() }, instructionCoverage.toSet(), score)
    }

    fun replaceFParameter(index: Int, newFParameter: FParameter): Seed {
        return Seed(
            thisInstance.copy(),
            parameters.mapIndexed { ind, fParameter -> if (ind == index) newFParameter else fParameter.copy() },
            instructionCoverage.toSet(),
            score
        )
    }
}