package org.utbot.testcheckers

import org.utbot.framework.PathSelectorType
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings

inline fun <reified T> withFeaturePath(featurePath: String, block: () -> T): T {
    val prevFeaturePath = UtSettings.featurePath
    val prevEnableFeatureProcess = UtSettings.enableFeatureProcess

    UtSettings.featurePath = featurePath
    UtSettings.enableFeatureProcess = true

    try {
        return block()
    } finally {
        UtSettings.featurePath = prevFeaturePath
        UtSettings.enableFeatureProcess = prevEnableFeatureProcess
    }
}

inline fun <reified T> withUsingReflectionForMaximizingCoverage(maximizeCoverage: Boolean, block: () -> T): T {
    val prev = UtSettings.maximizeCoverageUsingReflection
    UtSettings.maximizeCoverageUsingReflection = maximizeCoverage
    try {
        return block()
    } finally {
        UtSettings.maximizeCoverageUsingReflection = prev
    }
}

inline fun <reified T> withPathSelectorType(pathSelectorType: PathSelectorType, block: () -> T): T {
    val prev = UtSettings.pathSelectorType
    UtSettings.pathSelectorType = pathSelectorType
    try {
        return block()
    } finally {
        UtSettings.pathSelectorType = prev
    }
}

inline fun <reified T> withModelPath(modelPath: String, block: () -> T): T {
    val prev = UtSettings.modelPath
    UtSettings.modelPath = modelPath
    try {
        return block()
    } finally {
        UtSettings.modelPath = prev
    }
}

inline fun <reified T> withTreatingOverflowAsError(block: () -> T): T {
    val prev = UtSettings.treatOverflowAsError
    UtSettings.treatOverflowAsError = true
    try {
        return block()
    } finally {
        UtSettings.treatOverflowAsError = prev
    }
}

inline fun <reified T> withPushingStateFromPathSelectorForConcrete(block: () -> T): T {
    val prev = UtSettings.saveRemainingStatesForConcreteExecution
    UtSettings.saveRemainingStatesForConcreteExecution = true
    try {
        return block()
    } finally {
        UtSettings.saveRemainingStatesForConcreteExecution = prev
    }
}

inline fun <T> withoutSubstituteStaticsWithSymbolicVariable(block: () -> T) {
    val substituteStaticsWithSymbolicVariable = UtSettings.substituteStaticsWithSymbolicVariable
    UtSettings.substituteStaticsWithSymbolicVariable = false
    try {
        block()
    } finally {
        UtSettings.substituteStaticsWithSymbolicVariable = substituteStaticsWithSymbolicVariable
    }
}

inline fun <reified T> withoutMinimization(block: () -> T): T {
    val prev = UtSettings.testMinimizationStrategyType
    UtSettings.testMinimizationStrategyType = TestSelectionStrategyType.DO_NOT_MINIMIZE_STRATEGY
    try {
        return block()
    } finally {
        UtSettings.testMinimizationStrategyType = prev
    }
}

inline fun <reified T> withSolverTimeoutInMillis(timeoutInMillis: Int, block: () -> T): T {
    val prev = UtSettings.checkSolverTimeoutMillis
    UtSettings.checkSolverTimeoutMillis = timeoutInMillis
    try {
        return block()
    } finally {
        UtSettings.checkSolverTimeoutMillis = prev
    }
}

inline fun <reified T> withoutConcrete(block: () -> T): T {
    val prev = UtSettings.useConcreteExecution
    UtSettings.useConcreteExecution = false
    try {
        return block()
    } finally {
        UtSettings.useConcreteExecution = prev
    }
}

/**
 * Run [block] with disabled sandbox in the concrete executor
 */
inline fun <reified T> withoutSandbox(block: () -> T): T {
    val prev = UtSettings.useSandbox
    UtSettings.useSandbox = false
    try {
        return block()
    } finally {
        UtSettings.useSandbox = prev
    }
}
