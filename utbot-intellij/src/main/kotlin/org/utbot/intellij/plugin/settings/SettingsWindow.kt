package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selectedValueMatches
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
            codegenLanguageCombo = comboBox(DefaultComboBoxModel(CodegenLanguage.values()))
                .apply {
                component.renderer = CodeGenerationSettingItemRenderer()
                ContextHelpLabel.create("You can generate test methods in Java or Kotlin regardless of your source code language.")
            }.bindItem(
                getter = { settings.providerNameByServiceLoader(CodegenLanguage::class) as CodegenLanguage },
                setter = { settings.setProviderByLoader(CodegenLanguage::class, it as CodeGenerationSettingItem) }
            ).component
            codegenLanguageCombo.addActionListener {
                if (!codegenLanguageCombo.item.isSummarizationCompatible()) {
                    enableSummarizationGenerationCheckBox.isSelected = false
                }
            }
        }

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

        val valuesComboBox: (KClass<*>, Array<*>) -> Unit = { loader, values ->
            val serviceLabels = mapOf(
                RuntimeExceptionTestsBehaviour::class to "Tests with exceptions:",
                TreatOverflowAsError::class to "Overflow detection:",
                JavaDocCommentStyle::class to "Javadoc comment style:"
            )

            row(serviceLabels[loader] ?: error("Unknown service loader: $loader")) {
                comboBox(DefaultComboBoxModel(values))
                    .bindItem(
                        getter = { settings.providerNameByServiceLoader(loader) },
                        setter = { settings.setProviderByLoader(loader, it as CodeGenerationSettingItem) },
                    ).component.renderer = CodeGenerationSettingItemRenderer()
            }
        }

        mapOf(
            RuntimeExceptionTestsBehaviour::class to RuntimeExceptionTestsBehaviour.values(),
            TreatOverflowAsError::class to TreatOverflowAsError.values()
        ).forEach { (loader, values) ->
            valuesComboBox(loader, values)
        }

        row {
            runInspectionAfterTestGenerationCheckBox = checkBox("Display detected errors on the Problems tool window")
                .onApply {
                    settings.state.runInspectionAfterTestGeneration =
                        runInspectionAfterTestGenerationCheckBox.isSelected
                }
                .onReset {
                    runInspectionAfterTestGenerationCheckBox.isSelected =
                        settings.state.runInspectionAfterTestGeneration
                }
                .onIsModified {
                    runInspectionAfterTestGenerationCheckBox.isSelected xor settings.state.runInspectionAfterTestGeneration
                }
                .component
            contextHelp("Automatically run code inspection after test generation")
        }

        row {
            enableSummarizationGenerationCheckBox = checkBox("Enable summaries generation")
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
                }.enabledIf(codegenLanguageCombo.selectedValueMatches(CodegenLanguage?::isSummarizationCompatible))
                .component
        }

        valuesComboBox(JavaDocCommentStyle::class, JavaDocCommentStyle.values())

        row {
            forceMockCheckBox = checkBox("Force mocking static methods")
                .onApply {
                    settings.state.forceStaticMocking =
                        if (forceMockCheckBox.isSelected) ForceStaticMocking.FORCE else ForceStaticMocking.DO_NOT_FORCE
                }
                .onReset { forceMockCheckBox.isSelected = settings.forceStaticMocking == ForceStaticMocking.FORCE }
                .onIsModified { forceMockCheckBox.isSelected xor (settings.forceStaticMocking != ForceStaticMocking.DO_NOT_FORCE) }
                .component
            contextHelp("Overrides other mocking settings")
        }

        row("Classes to be forcedly mocked:") {}
        row {
            val updater = Runnable {
                UIUtil.setEnabled(excludeTable.component, forceMockCheckBox.isSelected, true)
            }
            cell(excludeTable.component)
                .align(Align.FILL)
                .onApply { excludeTable.apply() }
                .onReset {
                    excludeTable.reset()
                    updater.run()
                }
                .onIsModified { excludeTable.isModified() }

            forceMockCheckBox.addActionListener { updater.run() }
        }

        val granularity = 20
        var fuzzingPercent = 100.0 * settings.fuzzingValue
        val fuzzLabel = JBLabel("Fuzzing %.0f %%".format(fuzzingPercent))
        val symLabel = JBLabel("%.0f %% Symbolic execution".format(100.0 - fuzzingPercent))
        row("Test generation method:") {
            slider(0, granularity, 1, granularity / 4)
                .bindValue(
                    getter = { ((1 - settings.fuzzingValue) * granularity).toInt() },
                    setter = { settings.fuzzingValue = 1 - it / granularity.toDouble() }
                )
                .align(Align.FILL)
                .component.apply {
                    paintLabels = false
                    this.toolTipText =
                        "<html><body>While fuzzer \"guesses\" the values to enter as much execution paths as possible, symbolic executor tries to \"deduce\" them. Choose the proportion of generation time allocated for each of these methods within Test generation timeout. The slide has no effect for Spring Projects.</body></html>"
                    addChangeListener {
                        fuzzingPercent = 100.0 * ( granularity - value ) / granularity
                        fuzzLabel.text = "Fuzzing %.0f %%".format(fuzzingPercent)
                        symLabel.text = "%.0f %% Symbolic execution".format(100.0 - fuzzingPercent)
                    }
                }
        }.enabled(UtSettings.useFuzzing)
        indent{ indent{ indent {
            row {
                cell(BorderLayoutPanel().apply {
                    addToLeft(fuzzLabel)
                    addToRight(symLabel)
                }).align(Align.FILL)
            }
        }}}
        if (!UtSettings.useFuzzing) {
            row {
                comment("Fuzzing is disabled in configuration file.")
                link("Edit configuration") {
                    UIUtil.getWindow(fuzzLabel)?.dispose()
                    showSettingsEditor(project, "useFuzzing")
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
