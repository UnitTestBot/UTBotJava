package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.labelTable
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.slider
import com.intellij.ui.layout.withValueBinding
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class SettingsWindow(val project: Project) {
    private val settings = project.service<Settings>()
    // TODO it is better to use something like SearchEverywhere for classes but it is complicated to implement
    private val excludeTable = MockAlwaysClassesTable(project)

    val panel: JPanel = panel {
        val valuesComboBox: LayoutBuilder.(KClass<*>, Array<*>) -> Unit = { loader, values ->
            val serviceLabels = mapOf(
                MockStrategyApi::class to "Mock strategy:",
                CodegenLanguage::class to "Language generation:",
                RuntimeExceptionTestsBehaviour::class to "Test with exceptions:",
                ForceStaticMocking::class to "Force static mocking:",
                TreatOverflowAsError::class to "Overflow detection:",
            )

            val serviceComments = mapOf(
                RuntimeExceptionTestsBehaviour::class to "Test behavior when runtime exception occurs",
            )

            row(serviceLabels[loader] ?: error("Unknown service loader: $loader")) {
                cell {
                    comboBox(
                        DefaultComboBoxModel(values),
                        getter = { settings.providerNameByServiceLoader(loader) },
                        setter = { settings.setProviderByLoader(loader, it as CodeGenerationSettingItem) },
                    ).apply {
                        val comment = serviceComments[loader]
                        if (comment != null) {
                            ContextHelpLabel.create(comment)()
                        }
                    }
                }
            }
        }

        mapOf(
            MockStrategyApi::class to MockStrategyApi.values(),
            CodegenLanguage::class to CodegenLanguage.values(),
            RuntimeExceptionTestsBehaviour::class to RuntimeExceptionTestsBehaviour.values(),
            TreatOverflowAsError::class to TreatOverflowAsError.values()
        ).forEach { (loader, values) ->
            valuesComboBox(loader, values)
        }

        row("Hanging test timeout:") {
            cell {
                spinner(
                    getter = {
                        settings.hangingTestsTimeout.timeoutMs
                            .coerceIn(HangingTestsTimeout.MIN_TIMEOUT_MS, HangingTestsTimeout.MAX_TIMEOUT_MS).toInt()
                    },
                    setter = { settings.hangingTestsTimeout = HangingTestsTimeout(it.toLong()) },
                    minValue = HangingTestsTimeout.MIN_TIMEOUT_MS.toInt(),
                    maxValue = HangingTestsTimeout.MAX_TIMEOUT_MS.toInt(),
                    step = 50,
                )
                comment("milliseconds")
            }
        }

        mapOf(
            ForceStaticMocking::class to ForceStaticMocking.values(),
        ).forEach { (loader, values) ->
            valuesComboBox(loader, values)
        }

        row {
            excludeTable.component(CCFlags.grow)
                .onApply { excludeTable.apply() }
                .onReset { excludeTable.reset() }
                .onIsModified { excludeTable.isModified() }
        }

        row("Code analysis:") {
            enabled = UtSettings.useFuzzing
            val granularity = 60
            slider(0, granularity, 1, granularity / 4)
                .labelTable {
                    put(0, JLabel("Simpler"))
                    put(granularity, JLabel("Deeper"))
                }.withValueBinding(
                    PropertyBinding(
                        get = { ((1 - settings.fuzzingValue) * granularity).toInt() },
                        set = { settings.fuzzingValue = 1 - it / granularity.toDouble() }
                    )
                )
                .constraints(CCFlags.growX)
        }
    }

    fun isModified(): Boolean {
        return excludeTable.isModified() || (panel as DialogPanel).isModified()
    }

    fun apply() {
        excludeTable.apply()
        (panel as DialogPanel).apply()
    }

    fun reset() {
        excludeTable.reset()
        (panel as DialogPanel).reset()
    }
}