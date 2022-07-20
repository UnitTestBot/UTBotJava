package org.utbot.intellij.plugin.settings

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.intellij.plugin.ui.utils.labeled
import org.utbot.intellij.plugin.ui.utils.panelNoTitle
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Comparing
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class SettingsWindow(val project: Project) {
    var panel: JPanel

    private val settings = project.service<Settings>()
    private val comboBoxes: MutableList<ComboBoxInfo> = mutableListOf()

    private data class ComboBoxInfo(val loader: KClass<*>, val comboBox: ComboBox<CodeGenerationSettingItem>)

    private val mockStrategyComboBox = ComboBox(DefaultComboBoxModel(MockStrategyApi.values()))
    private val codegenLanguageComboBox = ComboBox(DefaultComboBoxModel(CodegenLanguage.values()))
    private val runtimeExceptionTestsBehaviourComboBox = ComboBox(
        DefaultComboBoxModel(RuntimeExceptionTestsBehaviour.values())
    )
    private val hangingTestTimeoutSecondsConfigurable = HangingTestTimeoutSecondsConfigurable()
    private val forceStaticMockingComboBox = ComboBox(DefaultComboBoxModel(ForceStaticMocking.values()))
    private val treatOverflowAsErrorComboBox = ComboBox(DefaultComboBoxModel(TreatOverflowAsError.values()))

    // TODO it is better to use something like SearchEverywhere for classes but it is complicated to implement
    private val excludeTable = MockAlwaysClassesTable(project)


    val isModified: Boolean
        get() = comboBoxes.any { isComboBoxModified(it) } ||
                excludeTable.isModified() ||
                hangingTestTimeoutSecondsConfigurable.isModified

    fun apply() {
        comboBoxes.forEach { (loader, comboBox) ->
            val newProvider = comboBox.selectedItem as CodeGenerationSettingItem
            settings.setProviderByLoader(loader, newProvider)
        }
        hangingTestTimeoutSecondsConfigurable.apply()
        excludeTable.apply()
    }

    fun reset() {
        comboBoxes.forEach { (loader, comboBox) ->
            comboBox.selectedItem = settings.providerNameByServiceLoader(loader)
        }
        excludeTable.reset()
        hangingTestTimeoutSecondsConfigurable.reset()
    }

    init {
        comboBoxes += ComboBoxInfo(
            MockStrategyApi::class,
            mockStrategyComboBox as ComboBox<CodeGenerationSettingItem>
        )
        comboBoxes += ComboBoxInfo(
            CodegenLanguage::class,
            codegenLanguageComboBox as ComboBox<CodeGenerationSettingItem>
        )
        comboBoxes += ComboBoxInfo(
            RuntimeExceptionTestsBehaviour::class,
            runtimeExceptionTestsBehaviourComboBox as ComboBox<CodeGenerationSettingItem>
        )
        comboBoxes += ComboBoxInfo(
            ForceStaticMocking::class,
            forceStaticMockingComboBox as ComboBox<CodeGenerationSettingItem>
        )
        comboBoxes += ComboBoxInfo(
            TreatOverflowAsError::class,
            treatOverflowAsErrorComboBox as ComboBox<CodeGenerationSettingItem>
        )

        panel = settingsPanel()
        panel.addAncestorListener(object : AncestorListener {
            private fun setItems() {
                mockStrategyComboBox.item = settings.mockStrategy
                codegenLanguageComboBox.item = settings.codegenLanguage
                runtimeExceptionTestsBehaviourComboBox.item = settings.runtimeExceptionTestsBehaviour
                forceStaticMockingComboBox.item = settings.forceStaticMocking
                treatOverflowAsErrorComboBox.item = settings.treatOverflowAsError
            }

            // get settings from project state on window appearance
            override fun ancestorAdded(event: AncestorEvent) {
                setItems()
            }

            override fun ancestorRemoved(event: AncestorEvent) {}

            override fun ancestorMoved(event: AncestorEvent) {}
        })
    }

    private fun settingsPanel(): JPanel = panel {
        comboBoxes
            .filterNot { it.loader == ForceStaticMocking::class }
            .forEach { (loader, comboBox) -> labeled(labelByServiceLoader(loader), comboBox) }

        row {
            val timeoutComponent = hangingTestTimeoutSecondsConfigurable.createComponent()
            hangingTestTimeoutSecondsConfigurable.reset()

            panelNoTitle(timeoutComponent)
        }

        labeled(labelByServiceLoader(ForceStaticMocking::class), forceStaticMockingComboBox)
        row {
            excludeTable.component(CCFlags.grow)
                .onApply { excludeTable.apply() }
                .onReset { excludeTable.reset() }
                .onIsModified { excludeTable.isModified() }
        }
    }

    private fun labelByServiceLoader(loader: KClass<*>): String =
        when (loader) {
            MockStrategyApi::class -> "Mock strategy: "
            CodegenLanguage::class -> "Language generation: "
            RuntimeExceptionTestsBehaviour::class -> "Behavior of the tests producing Runtime exceptions: "
            ForceStaticMocking::class -> "Force static mocking: "
            TreatOverflowAsError::class -> "Overflow detection: "
            // TODO: add error processing
            else -> error("Unknown service loader: $loader")
        }

    private fun isComboBoxModified(info: ComboBoxInfo): Boolean =
        settings.providerNameByServiceLoader(info.loader) != info.comboBox.selectedItem

    private inner class HangingTestTimeoutSecondsConfigurable : com.intellij.openapi.options.Configurable {
        private val title = "Hanging test timeout"
        private val timeUnit = "ms"
        private val spinnerStepSize = 50L

        private lateinit var timeoutSpinner: JSpinner

        override fun createComponent(): JComponent {
            val wrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            val titleLabel = JLabel(title)
            wrapper.add(titleLabel)
            timeoutSpinner = JSpinner(createSpinnerModel())
            wrapper.add(timeoutSpinner)
            val timeUnitLabel = JLabel(timeUnit)
            timeUnitLabel.border = JBUI.Borders.empty(0, 1)
            wrapper.add(timeUnitLabel)

            return wrapper
        }

        fun createSpinnerModel(): SpinnerNumberModel = SpinnerNumberModel(
            HangingTestsTimeout.DEFAULT_TIMEOUT_MS,
            HangingTestsTimeout.MIN_TIMEOUT_MS,
            HangingTestsTimeout.MAX_TIMEOUT_MS,
            spinnerStepSize
        )

        override fun isModified(): Boolean =
            !Comparing.equal(timeoutSpinner.value, settings.hangingTestsTimeout.timeoutMs)

        override fun apply() {
            val hangingTestsTimeout = HangingTestsTimeout(timeoutSpinner.value as Long)

            settings.hangingTestsTimeout = hangingTestsTimeout
        }

        override fun getDisplayName(): String = "Test milliseconds timeout"

        override fun reset() {
            timeoutSpinner.value = settings.hangingTestsTimeout.timeoutMs
        }
    }
}