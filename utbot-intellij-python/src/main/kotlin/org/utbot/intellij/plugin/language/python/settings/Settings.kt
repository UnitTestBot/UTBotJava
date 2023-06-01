package org.utbot.intellij.plugin.language.python.settings

import org.utbot.intellij.plugin.language.python.PythonTestsModel
import org.utbot.intellij.plugin.settings.Settings

fun loadStateFromModel(settings: Settings, model: PythonTestsModel) {
    settings.loadState(fromGenerateTestsModel(model))
}

private fun fromGenerateTestsModel(model: PythonTestsModel): Settings.State {
    return Settings.State(
        sourceRootHistory = model.sourceRootHistory,
        testFramework = model.testFramework,
        generationTimeoutInMillis = model.timeout,
        enableExperimentalLanguagesSupport = true,
    )
}
