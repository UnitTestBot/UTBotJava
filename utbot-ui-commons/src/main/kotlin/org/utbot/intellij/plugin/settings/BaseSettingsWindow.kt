package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.*
import kotlin.reflect.KClass
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.intellij.plugin.ui.components.CodeGenerationSettingItemRenderer

class BaseSettingsWindow(val project: Project) {
    private val settings = project.service<Settings>()

    private lateinit var enableExperimentalLanguagesCheckBox: JCheckBox

    private fun Row.createCombo(loader: KClass<*>, values: Array<*>) {
        comboBox(DefaultComboBoxModel(values))
            .bindItem(
                getter = { settings.providerNameByServiceLoader(loader) },
                setter = { settings.setProviderByLoader(loader, it as CodeGenerationSettingItem) },
            ).component.renderer = CodeGenerationSettingItemRenderer()
    }

    val panel: JPanel = panel {
        row {
            enableExperimentalLanguagesCheckBox = checkBox("Experimental languages support")
                .onApply {
                    settings.state.enableExperimentalLanguagesSupport =
                        enableExperimentalLanguagesCheckBox.isSelected
                }
                .onReset {
                    enableExperimentalLanguagesCheckBox.isSelected =
                        settings.experimentalLanguagesSupport == true
                }
                .onIsModified { enableExperimentalLanguagesCheckBox.isSelected xor settings.experimentalLanguagesSupport }
                .component
            contextHelp("Enable JavaScript and Python if IDE supports them")
        }.bottomGap(BottomGap.MEDIUM)

        row("Tests with exceptions:") {
            createCombo(RuntimeExceptionTestsBehaviour::class, RuntimeExceptionTestsBehaviour.values())
        }

        row("Hanging test timeout:") {
            spinner(
                range = IntRange(
                    HangingTestsTimeout.MIN_TIMEOUT_MS.toInt(),
                    HangingTestsTimeout.MAX_TIMEOUT_MS.toInt()
                ),
                step = 50
            ).bindIntValue(
                getter = {
                    settings.hangingTestsTimeout.timeoutMs
                        .coerceIn(HangingTestsTimeout.MIN_TIMEOUT_MS, HangingTestsTimeout.MAX_TIMEOUT_MS).toInt()
                },
                setter = {
                    settings.hangingTestsTimeout = HangingTestsTimeout(it.toLong())
                }
            )

            label("milliseconds per method")
            contextHelp(
                "Set this timeout to define which test is \"hanging\". Increase it to test the " +
                        "time-consuming method or decrease if the execution speed is critical for you."
            )
        }
    }

    fun isModified(): Boolean {
        return (panel as DialogPanel).isModified()
    }

    fun apply() {
        (panel as DialogPanel).apply()
    }

    fun reset() {
        (panel as DialogPanel).reset()
    }
}
