package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.labelTable
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.slider
import com.intellij.ui.layout.withValueBinding
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JPanel
import kotlin.reflect.KClass
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.JavaDocCommentStyle
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.intellij.plugin.ui.components.CodeGenerationSettingItemRenderer
import javax.swing.JSlider

class SettingsWindow(val project: Project) {
    private val settings = project.service<Settings>()

    // TODO it is better to use something like SearchEverywhere for classes but it is complicated to implement
    private val excludeTable = MockAlwaysClassesTable(project)
    private lateinit var forceMockCheckBox: JCheckBox

    val panel: JPanel = panel {
        val valuesComboBox: LayoutBuilder.(KClass<*>, Array<*>) -> Unit = { loader, values ->
            val serviceLabels = mapOf(
                CodegenLanguage::class to "Generated test language:",
                RuntimeExceptionTestsBehaviour::class to "Tests with exceptions:",
                TreatOverflowAsError::class to "Overflow detection:",
                JavaDocCommentStyle::class to "Javadoc comment style:"
            )
            val tooltipLabels = mapOf(
                CodegenLanguage::class to "You can generate test methods in Java or Kotlin regardless of your source code language."
            )

            row(serviceLabels[loader] ?: error("Unknown service loader: $loader")) {
                cell {
                    comboBox(
                        DefaultComboBoxModel(values),
                        getter = { settings.providerNameByServiceLoader(loader) },
                        setter = { settings.setProviderByLoader(loader, it as CodeGenerationSettingItem) },
                    ).apply {
                        component.renderer = CodeGenerationSettingItemRenderer()
                        ContextHelpLabel.create(tooltipLabels[loader] ?: return@apply)()
                    }
                }
            }
        }

        valuesComboBox(CodegenLanguage::class, CodegenLanguage.values())

        row("Hanging test timeout:") {
            cell {
                spinner(
                    getter = {
                        settings.hangingTestsTimeout.timeoutMs
                            .coerceIn(HangingTestsTimeout.MIN_TIMEOUT_MS, HangingTestsTimeout.MAX_TIMEOUT_MS).toInt()
                    },
                    setter = {
                        settings.hangingTestsTimeout = HangingTestsTimeout(it.toLong())
                    },
                    minValue = HangingTestsTimeout.MIN_TIMEOUT_MS.toInt(),
                    maxValue = HangingTestsTimeout.MAX_TIMEOUT_MS.toInt(),
                    step = 50,
                )

                label("milliseconds per method")
                    .apply {
                        ContextHelpLabel.create(
                            "Set this timeout to define which test is \"hanging\". Increase it to test the " +
                                    "time-consuming method or decrease if the execution speed is critical for you."
                        )()
                    }
            }
        }

        mapOf(
            RuntimeExceptionTestsBehaviour::class to RuntimeExceptionTestsBehaviour.values(),
            TreatOverflowAsError::class to TreatOverflowAsError.values(),
            JavaDocCommentStyle::class to JavaDocCommentStyle.values()
        ).forEach { (loader, values) ->
            valuesComboBox(loader, values)
        }

        row {
            cell {
                forceMockCheckBox = checkBox("Force mocking static methods")
                    .onApply {
                        settings.state.forceStaticMocking =
                            if (forceMockCheckBox.isSelected) ForceStaticMocking.FORCE else ForceStaticMocking.DO_NOT_FORCE
                    }
                    .onReset { forceMockCheckBox.isSelected = settings.forceStaticMocking == ForceStaticMocking.FORCE }
                    .onIsModified { forceMockCheckBox.isSelected xor (settings.forceStaticMocking != ForceStaticMocking.DO_NOT_FORCE) }
                    .apply { ContextHelpLabel.create("Overrides other mocking settings")() }
                    .component
            }
        }

        row("Classes to be forcedly mocked:") {}
        row {
            val excludeTableCellBuilder = excludeTable.component(CCFlags.grow)
            val updater = Runnable {
                UIUtil.setEnabled(excludeTableCellBuilder.component, forceMockCheckBox.isSelected, true)
            }
            excludeTableCellBuilder
                .onApply { excludeTable.apply() }
                .onReset {
                    excludeTable.reset()
                    updater.run()
                }
                .onIsModified { excludeTable.isModified() }
            forceMockCheckBox.addActionListener { updater.run() }


        }

        val fuzzLabel = JBLabel("Fuzzing")
        val symLabel = JBLabel("Symbolic execution")
        row("Test generation method:") {
            enabled = UtSettings.useFuzzing
            val granularity = 20
            slider(0, granularity, 1, granularity / 4)
                .labelTable {
                    // clear all labels
                }.withValueBinding(
                    PropertyBinding(
                        get = { ((1 - settings.fuzzingValue) * granularity).toInt() },
                        set = { settings.fuzzingValue = 1 - it / granularity.toDouble() }
                    )
                )
                .constraints(CCFlags.growX)
                .component.castSafelyTo<JSlider>()?.apply {
                    toolTipText = "<html><body>While fuzzer \"guesses\" the values to enter as much execution paths as possible, symbolic executor tries to \"deduce\" them. Choose the proportion of generation time allocated for each of these methods within Test generation timeout</body></html>"
                    addChangeListener {
                        fuzzLabel.text = "Fuzzing " + "%.0f %%".format(100.0 * (granularity - value) / granularity)
                        symLabel.text = "%.0f %%".format(100.0 * value / granularity) + " Symbolic execution"
                    }
                }
        }
        row("") {
            BorderLayoutPanel().apply {
                addToLeft(fuzzLabel)
                addToRight(symLabel)
            }().constraints(CCFlags.growX)
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
