package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.labelTable
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selectedValueMatches
import com.intellij.ui.layout.slider
import com.intellij.ui.layout.withValueBinding
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.utbot.framework.SummariesGenerationType
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.JavaDocCommentStyle
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.framework.plugin.api.isSummarizationCompatible
import org.utbot.intellij.plugin.ui.components.CodeGenerationSettingItemRenderer
import org.utbot.intellij.plugin.util.showSettingsEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JPanel
import kotlin.reflect.KClass

class SettingsWindow(val project: Project) {
    private val settings = project.service<Settings>()

    // TODO it is better to use something like SearchEverywhere for classes but it is complicated to implement
    private lateinit var codegenLanguageCombo: ComboBox<CodegenLanguage>
    private val excludeTable = MockAlwaysClassesTable(project)
    private lateinit var runInspectionAfterTestGenerationCheckBox: JCheckBox
    private lateinit var forceMockCheckBox: JCheckBox
    private lateinit var enableSummarizationGenerationCheckBox: JCheckBox
    private lateinit var enableExperimentalLanguagesCheckBox: JCheckBox

    val panel: JPanel = panel {
        row("Generated test language:") {
            cell {
                codegenLanguageCombo = comboBox(
                    DefaultComboBoxModel(CodegenLanguage.values()),
                    getter = { settings.providerNameByServiceLoader(CodegenLanguage::class) as CodegenLanguage },
                    setter = { settings.setProviderByLoader(CodegenLanguage::class, it as CodeGenerationSettingItem) }
                ).apply {
                    component.renderer = CodeGenerationSettingItemRenderer()
                    ContextHelpLabel.create("You can generate test methods in Java or Kotlin regardless of your source code language.")
                }.component
                codegenLanguageCombo.addActionListener {
                    if (!codegenLanguageCombo.item.isSummarizationCompatible()) {
                        enableSummarizationGenerationCheckBox.isSelected = false
                    }
                }
            }
        }
        val valuesComboBox: LayoutBuilder.(KClass<*>, Array<*>) -> Unit = { loader, values ->
            val serviceLabels = mapOf(
                RuntimeExceptionTestsBehaviour::class to "Tests with exceptions:",
                TreatOverflowAsError::class to "Overflow detection:",
                JavaDocCommentStyle::class to "Javadoc comment style:"
            )

            row(serviceLabels[loader] ?: error("Unknown service loader: $loader")) {
                cell {
                    comboBox(
                        DefaultComboBoxModel(values),
                        getter = { settings.providerNameByServiceLoader(loader) },
                        setter = { settings.setProviderByLoader(loader, it as CodeGenerationSettingItem) },
                    ).apply {
                        component.renderer = CodeGenerationSettingItemRenderer()
                    }
                }
            }
        }

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
                runInspectionAfterTestGenerationCheckBox = checkBox("Display detected errors on the Problems tool window")
                    .onApply {
                        settings.state.runInspectionAfterTestGeneration = runInspectionAfterTestGenerationCheckBox.isSelected
                    }
                    .onReset {
                        runInspectionAfterTestGenerationCheckBox.isSelected = settings.state.runInspectionAfterTestGeneration
                    }
                    .onIsModified {
                        runInspectionAfterTestGenerationCheckBox.isSelected xor settings.state.runInspectionAfterTestGeneration
                    }
                    // .apply { ContextHelpLabel.create("Automatically run code inspection after test generation")() }
                    .component
            }
        }

        row {
            cell {
                enableSummarizationGenerationCheckBox = checkBox("Enable Summaries Generation")
                    .onApply {
                        settings.state.summariesGenerationType =
                            if (enableSummarizationGenerationCheckBox.isSelected) SummariesGenerationType.FULL else SummariesGenerationType.NONE
                    }
                    .onReset {
                        enableSummarizationGenerationCheckBox.isSelected =
                            settings.state.summariesGenerationType != SummariesGenerationType.NONE
                    }
                    .onIsModified {
                        enableSummarizationGenerationCheckBox.isSelected xor (settings.state.summariesGenerationType != SummariesGenerationType.NONE)
                    }
                    .enableIf(codegenLanguageCombo.selectedValueMatches(CodegenLanguage?::isSummarizationCompatible))
                    .component
            }
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

        row {
            cell {
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
                    .apply { ContextHelpLabel.create("Enable JavaScript and Python if IDE supports them")() }
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
                .component.apply {
                    toolTipText = "<html><body>While fuzzer \"guesses\" the values to enter as much execution paths as possible, symbolic executor tries to \"deduce\" them. Choose the proportion of generation time allocated for each of these methods within Test generation timeout. The slide has no effect for Spring Projects.</body></html>"
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
        if (!UtSettings.useFuzzing) {
            row("") {
                cell {
                    component(comment("Fuzzing is disabled in configuration file.").component)
                    component(ActionLink("Edit configuration") {
                        UIUtil.getWindow(fuzzLabel)?.dispose()
                        showSettingsEditor(project, "useFuzzing")
                    })
                }
            }
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
