@file:Suppress("MemberVisibilityCanBePrivate")

package org.utbot.intellij.plugin.settings

import org.utbot.framework.plugin.api.MockFramework
import org.utbot.intellij.plugin.models.GenerateTestsModel

fun loadStateFromModel(settings: Settings, model: GenerateTestsModel) {
    settings.loadState(fromGenerateTestsModel(model))
}

private fun fromGenerateTestsModel(model: GenerateTestsModel): Settings.State {
    return Settings.State(
        sourceRootHistory = model.sourceRootHistory,
        codegenLanguage = model.codegenLanguage,
        testFramework = JavaTestFrameworkMapper.toString(model.testFramework),
        mockStrategy = model.mockStrategy,
        mockFramework = model.mockFramework ?: MockFramework.defaultItem,
        staticsMocking = model.staticsMocking,
        runtimeExceptionTestsBehaviour = model.runtimeExceptionTestsBehaviour,
        hangingTestsTimeout = model.hangingTestsTimeout,
        runInspectionAfterTestGeneration = model.runInspectionAfterTestGeneration,
        forceStaticMocking = model.forceStaticMocking,
        parametrizedTestSource = model.parametrizedTestSource,
        classesToMockAlways = model.chosenClassesToMockAlways.mapTo(mutableSetOf()) { it.name }.toTypedArray(),
        springTestsType = model.springTestsType,
        fuzzingValue = model.fuzzingValue,
        runGeneratedTestsWithCoverage = model.runGeneratedTestsWithCoverage,
        commentStyle = model.commentStyle,
        generationTimeoutInMillis = model.timeout,
        summariesGenerationType = model.summariesGenerationType
    )
}