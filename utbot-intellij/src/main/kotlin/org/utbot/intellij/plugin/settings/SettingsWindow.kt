package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selected
import com.intellij.ui.layout.selectedValueMatches
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.*
import kotlin.reflect.KClass
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

class SettingsWindow(val project: Project) {
    private val settings = project.service<Settings>()

    // TODO it is better to use something like SearchEverywhere for classes but it is complicated to implement
    private lateinit var codegenLanguageCombo: ComboBox<CodegenLanguage>
    private val excludeTable = MockAlwaysClassesTable(project)
    private lateinit var useTaintAnalysisCheckBox: JCheckBox
    private lateinit var runInspectionAfterTestGenerationCheckBox: JCheckBox
    private lateinit var forceMockCheckBox: JCheckBox
    private lateinit var enableSummarizationGenerationCheckBox: JCheckBox
    private lateinit var enableExperimentalLanguagesCheckBox: JCheckBox

    private fun Row.createCombo(loader: KClass<*>, values: Array<*>) {
        comboBox(DefaultComboBoxModel(values))
            .bindItem(
                getter = { settings.providerNameByServiceLoader(loader) },
                setter = { settings.setProviderByLoader(loader, it as CodeGenerationSettingItem) },
            ).component.renderer = CodeGenerationSettingItemRenderer()
    }

    val panel: JPanel = panel {
        row("Generated test language:") {
            codegenLanguageCombo = comboBox(DefaultComboBoxModel(CodegenLanguage.values())).gap(RightGap.COLUMNS)
                .apply {
                    component.renderer = CodeGenerationSettingItemRenderer()
                    ContextHelpLabel.create("You can generate test methods in Java or Kotlin regardless of your source code language.")
                }.bindItem(
                    getter = { settings.providerNameByServiceLoader(CodegenLanguage::class) as CodegenLanguage },
                    setter = {
                        settings.setProviderByLoader(
                            CodegenLanguage::class,
                            it as CodeGenerationSettingItem
                        )
                    }
                ).component
            codegenLanguageCombo.addActionListener {
                if (!codegenLanguageCombo.item.isSummarizationCompatible()) {
                    enableSummarizationGenerationCheckBox.isSelected = false
                }
            }

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
        row("Overflow detection:") {
            createCombo(TreatOverflowAsError::class, TreatOverflowAsError.values())
        }
        row {
            useTaintAnalysisCheckBox =
                checkBox("Enable taint analysis")
                    .onApply {
                        settings.state.useTaintAnalysis = useTaintAnalysisCheckBox.isSelected
                    }
                    .onReset {
                        useTaintAnalysisCheckBox.isSelected = settings.state.useTaintAnalysis
                    }
                    .onIsModified {
                        useTaintAnalysisCheckBox.isSelected xor settings.state.useTaintAnalysis
                    }
                    .component
            contextHelp("Experimental taint analysis support")
        }
        row {
            runInspectionAfterTestGenerationCheckBox =
                checkBox("Display detected errors on the Problems tool window")
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
        indent {
            row("Javadoc comment style:") {
                createCombo(JavaDocCommentStyle::class, JavaDocCommentStyle.values())
            }.enabledIf(enableSummarizationGenerationCheckBox.selected).bottomGap(BottomGap.MEDIUM)
        }

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
        }.bottomGap(BottomGap.MEDIUM)

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
        val fuzzLabel = JBLabel("Fuzzing")
        val symLabel = JBLabel("Symbolic execution")
        row {
            cell(BorderLayoutPanel().apply {
                topGap(TopGap.SMALL)
                addToLeft(JBLabel("Test generation method:").apply { verticalAlignment = SwingConstants.TOP })
                addToCenter(BorderLayoutPanel().apply {
                    val granularity = 20
                    val slider = object : JSlider() {
                        val updater = Runnable() {
                            val fuzzingPercent = 100.0 * (granularity - value) / granularity
                            fuzzLabel.text = "Fuzzing %.0f %%".format(fuzzingPercent)
                            symLabel.text = "%.0f %% Symbolic execution".format(100.0 - fuzzingPercent)
                        }

                        override fun getValue() = ((1 - settings.fuzzingValue) * granularity).toInt()

                        override fun setValue(n: Int) {
                            val tmp = value
                            settings.fuzzingValue = 1 - n / granularity.toDouble()
                            if (tmp != n) {
                                updater.run()
                            }
                        }
                    }
                    UIUtil.setSliderIsFilled(slider, true)
                    slider.minimum = 0
                    slider.maximum = granularity
                    slider.minorTickSpacing = 1
                    slider.majorTickSpacing = granularity / 4
                    slider.paintTicks = true
                    slider.paintTrack = true
                    slider.paintLabels = false
                    slider.toolTipText =
                        "<html><body>While fuzzer \"guesses\" the values to enter as much execution paths as possible, symbolic executor tries to \"deduce\" them. Choose the proportion of generation time allocated for each of these methods within Test generation timeout. The slide has no effect for Spring Projects.</body></html>"
                    slider.updater.run()
                    addToTop(slider)
                    addToBottom(BorderLayoutPanel().apply {
                        addToLeft(fuzzLabel)
                        addToRight(symLabel)
                    })
                })
            }).align(Align.FILL)
        }.enabled(UtSettings.useFuzzing)
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
