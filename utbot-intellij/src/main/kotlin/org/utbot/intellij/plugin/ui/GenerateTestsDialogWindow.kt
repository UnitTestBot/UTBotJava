@file:Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")

package org.utbot.intellij.plugin.ui

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectNewWindowDoNotAskOption
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModificationService
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor
import com.intellij.openapi.roots.ui.configuration.ComboBoxWithSeparators
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.OptionAction
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore.urlToPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.ui.MemberSelectionTable
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.IdeBorderFactory.createBorder
import com.intellij.ui.InplaceButton
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Panel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.enableIf
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selectedValueMatches
import com.intellij.util.IncorrectOperationException
import com.intellij.util.io.exists
import com.intellij.util.lang.JavaVersion
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Borders.merge
import com.intellij.util.ui.JBUI.scale
import com.intellij.util.ui.JBUI.size
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import mu.KotlinLogging
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.ParseException
import java.util.concurrent.TimeUnit
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.text.DefaultFormatter
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.thenRun
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.DependencyInjectionFramework
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.NoStaticMocking
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.SpringBeans
import org.utbot.framework.codegen.domain.TypeReplacementApproach
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockFramework.MOCKITO
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.framework.plugin.api.isSummarizationCompatible
import org.utbot.framework.plugin.api.utils.MOCKITO_EXTENSIONS_FILE_CONTENT
import org.utbot.framework.plugin.api.utils.MOCKITO_EXTENSIONS_FOLDER
import org.utbot.framework.plugin.api.utils.MOCKITO_MOCKMAKER_FILE_NAME
import org.utbot.framework.util.Conflict
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.models.id
import org.utbot.intellij.plugin.models.jUnit4LibraryDescriptor
import org.utbot.intellij.plugin.models.jUnit5LibraryDescriptor
import org.utbot.intellij.plugin.models.jUnit5ParametrizedTestsLibraryDescriptor
import org.utbot.intellij.plugin.models.mockitoCoreLibraryDescriptor
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.models.testNgNewLibraryDescriptor
import org.utbot.intellij.plugin.models.testNgOldLibraryDescriptor
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.settings.loadStateFromModel
import org.utbot.intellij.plugin.ui.components.CodeGenerationSettingItemRenderer
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import org.utbot.intellij.plugin.ui.utils.LibrarySearchScope
import org.utbot.intellij.plugin.ui.utils.addSourceRootIfAbsent
import org.utbot.intellij.plugin.ui.utils.allLibraries
import org.utbot.intellij.plugin.ui.utils.createTestFrameworksRenderer
import org.utbot.intellij.plugin.ui.utils.findDependencyInjectionLibrary
import org.utbot.intellij.plugin.ui.utils.findFrameworkLibrary
import org.utbot.intellij.plugin.ui.utils.findParametrizedTestsLibrary
import org.utbot.intellij.plugin.ui.utils.getOrCreateTestResourcesPath
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.ui.utils.kotlinTargetPlatform
import org.utbot.intellij.plugin.ui.utils.parseVersion
import org.utbot.intellij.plugin.ui.utils.testResourceRootTypes
import org.utbot.intellij.plugin.ui.utils.testRootType
import org.utbot.intellij.plugin.util.IntelliJApiHelper
import org.utbot.intellij.plugin.util.extractFirstLevelMembers
import org.utbot.intellij.plugin.util.findSdkVersion
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private const val RECENTS_KEY = "org.utbot.recents"

private const val SAME_PACKAGE_LABEL = "same as for sources"

private const val WILL_BE_INSTALLED_LABEL = " (will be installed)"

private const val NO_SPRING_CONFIGURATION_OPTION = "No configuration"

private const val ACTION_GENERATE = "Generate Tests"
private const val ACTION_GENERATE_AND_RUN = "Generate and Run"

class GenerateTestsDialogWindow(val model: GenerateTestsModel) : DialogWrapper(model.project) {
    companion object {
        const val minSupportedSdkVersion = 8
        const val maxSupportedSdkVersion = 17
    }

    private val logger = KotlinLogging.logger {}

    private val membersTable = MemberSelectionTable(emptyList(), null)

    private val cbSpecifyTestPackage = CheckBox("Specify destination package", false)
    private val testPackageField = PackageNameReferenceEditorCombo(
        findTestPackageComboValue(),
        model.project,
        RECENTS_KEY,
        "Choose Destination Package"
    )

    private val testSourceFolderField = TestFolderComboWithBrowseButton(model)

    private val codegenLanguages = createComboBox(CodegenLanguage.values())
    private val testFrameworks = createComboBox(TestFramework.allItems.toTypedArray())

    private val modelSpringConfigs = setOf(
        null to listOf(NO_SPRING_CONFIGURATION_OPTION),
        "Java-based configurations" to model.getSortedSpringConfigurationClasses(),
        "XML-based configurations" to model.getSpringXMLConfigurationFiles()
    )
    private val springConfig = createComboBoxWithSeparatorsForSpringConfigs(modelSpringConfigs)

    private val mockStrategies = createComboBox(MockStrategyApi.values())
    private val staticsMocking = JCheckBox("Mock static methods")
    private val timeoutSpinner =
        JBIntSpinner(TimeUnit.MILLISECONDS.toSeconds(model.timeout).toInt(), 1, Int.MAX_VALUE, 1).also {
            when(val editor = it.editor) {
                is JSpinner.DefaultEditor -> {
                    when(val formatter = editor.textField.formatter) {
                        is DefaultFormatter -> {formatter.allowsInvalid = false}
                    }
                }
            }
        }
    private val parametrizedTestSources = JCheckBox("Parameterized tests")

    private lateinit var panel: DialogPanel

    @Suppress("UNCHECKED_CAST")
    private val itemsToHelpTooltip = hashMapOf(
        (codegenLanguages as ComboBox<CodeGenerationSettingItem>) to createHelpLabel(),
        (testFrameworks as ComboBox<CodeGenerationSettingItem>) to createHelpLabel(),
        staticsMocking to null,
        parametrizedTestSources to null
    )

    private fun <T : CodeGenerationSettingItem> createComboBox(values: Array<T>) : ComboBox<T> {
        val comboBox = object:ComboBox<T>(DefaultComboBoxModel(values)) {
            var maxWidth = 0
            // Do not shrink strategy
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                if (size.width > maxWidth) maxWidth = size.width
                return size.apply { width = maxWidth }
            }
        }
        return comboBox.also {
            it.renderer = CodeGenerationSettingItemRenderer()
        }
    }

    private fun <T> createComboBoxWithSeparatorsForSpringConfigs(
        separatorToValues: Collection<Pair<T?, Collection<T>>>,
        width: Int = 300
    ): ComboBoxWithSeparators<T> {
        val comboBox = object : ComboBoxWithSeparators<T>() {}.apply {
            fun wrapInEntryModel(value: T) = object : ComboBoxWithSeparators<T>.EntryModel<T>(value) {
                override fun getPresentableText(): String = value.toString()
            }

            setMinimumAndPreferredWidth(width)
            separatorToValues.forEach { (separator, values) ->
                if (values.isEmpty()) return@forEach
                separator?.let { addItem(Separator(it.toString())) }
                values.forEach { value ->
                    addItem(wrapInEntryModel(value))
                }
            }
        }

        return comboBox
    }

    private fun createHelpLabel(commonTooltip: String? = null) = JBLabel(AllIcons.General.ContextHelp).apply {
        if (!commonTooltip.isNullOrEmpty()) toolTipText = commonTooltip
    }

    init {
        title = "Generate Tests with UnitTestBot"
        setResizable(false)

        TestFramework.allItems.forEach {
            it.isInstalled = findFrameworkLibrary(model.project, model.testModule, it) != null
            it.isParametrizedTestsConfigured = findParametrizedTestsLibrary(model.project, model.testModule, it) != null
        }
        MockFramework.allItems.forEach {
            it.isInstalled = findFrameworkLibrary(model.project, model.testModule, it) != null
        }
        DependencyInjectionFramework.allItems.forEach {
            it.isInstalled = findDependencyInjectionLibrary(model.project, model.testModule, it) != null
        }
        model.projectType = if (SpringBeans.isInstalled) ProjectType.Spring else ProjectType.PureJvm

        StaticsMocking.allItems.forEach {
            it.isConfigured = staticsMockingConfigured()
        }

        // Configure notification urls callbacks
        TestsReportNotifier.urlOpeningListener.callbacks[TestReportUrlOpeningListener.mockitoSuffix]?.plusAssign {
            configureMockFramework()
        }

        TestsReportNotifier.urlOpeningListener.callbacks[TestReportUrlOpeningListener.mockitoInlineSuffix]?.plusAssign {
            configureStaticMocking()
        }

        TestReportUrlOpeningListener.callbacks[TestReportUrlOpeningListener.eventLogSuffix]?.plusAssign {
            with(model.project) {
                if (this.isDisposed) return@with
                val twm = ToolWindowManager.getInstance(this)
                twm.getToolWindow("Event Log")?.activate(null)
            }
        }

        model.runGeneratedTestsWithCoverage = model.project.service<Settings>().runGeneratedTestsWithCoverage

        init()
    }


    @Suppress("UNCHECKED_CAST")
    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test sources root:") {
                component(testSourceFolderField)
            }
            row("Testing framework:") {
                makePanelWithHelpTooltip(
                    testFrameworks,
                    null
                )
            }
            if (model.projectType == ProjectType.Spring) {
                row("Spring configuration:") {
                    makePanelWithHelpTooltip(
                        springConfig,
                        ContextHelpLabel.create(
                            "100% Symbolic execution mode.\n" +
                                    "Classes defined in Spring configuration will be used instead " +
                                    "of interfaces and abstract classes.\n" +
                                    "Mocks will be used when necessary."
                        )
                    )
                }
            }
            row("Mocking strategy:") {
                makePanelWithHelpTooltip(
                    mockStrategies,
                    ContextHelpLabel.create("Mock everything around the target class or the whole package except the system classes. " +
                            "Otherwise, mock nothing. Mockito will be installed, if you don't have one.")
                )
            }.enableIf(ComboBoxPredicate(springConfig) {
                model.projectType != ProjectType.Spring || springConfig.item.getItem() == NO_SPRING_CONFIGURATION_OPTION
            })
            row { component(staticsMocking)}
            row {
                cell {
                    component(parametrizedTestSources)
                    component(ContextHelpLabel.create("Parametrization is not supported in some configurations, e.g. if mocks are used."))
                }
            }
            row("Test generation timeout:") {
                cell {
                    component(timeoutSpinner)
                    label("seconds per class")
                    component(ContextHelpLabel.create("Set the timeout for all test generation processes per class to complete."))
                }
            }

            row("Generate tests for:") {}
            row {
                scrollPane(membersTable)
            }
        }

        initDefaultValues()
        setListeners()
        updateMembersTable()
        return panel
    }

    private inline fun Cell.panelWithHelpTooltip(tooltipText: String?, crossinline init: Cell.() -> Unit): Cell {
        init()
        tooltipText?.let { component(ContextHelpLabel.create(it)) }
        return this
    }

    private fun Row.makePanelWithHelpTooltip(
        mainComponent: JComponent,
        label: JBLabel?
    ): CellBuilder<JPanel> =
        component(Panel().apply {
            add(mainComponent, BorderLayout.LINE_START)
            label?.let { add(it, BorderLayout.LINE_END) }
        })

    override fun createTitlePane(): JComponent? {
        val sdkVersion = findSdkVersion(model.srcModule)
        // TODO:SAT-1571 investigate Android Studio specific sdk issues
        if (sdkVersion.feature in minSupportedSdkVersion..maxSupportedSdkVersion || IntelliJApiHelper.isAndroidStudio()) return null
        isOKActionEnabled = false
        return SdkNotificationPanel(model, sdkVersion)
    }

    private fun findTestPackageComboValue(): String {
        return if (!model.isMultiPackage) {
            model.srcClasses.first().packageName
        } else {
            SAME_PACKAGE_LABEL
        }
    }

    /**
     * A panel to inform user about incorrect jdk in project.
     *
     * Note: this implementation was encouraged by NonModalCommitPromoter.
     */
    private inner class SdkNotificationPanel(
        private val model: GenerateTestsModel,
        private val sdkVersion: JavaVersion?,
    ) : BorderLayoutPanel() {
        init {
            border = merge(empty(10), createBorder(JBColor.border(), SideBorder.BOTTOM), true)

            addToLeft(JBLabel().apply {
                icon = AllIcons.Ide.FatalError
                text = if (sdkVersion != null) {
                    "SDK version $sdkVersion is not supported, use ${JavaSdkVersion.JDK_1_8}, ${JavaSdkVersion.JDK_11} or ${JavaSdkVersion.JDK_17}"
                } else {
                    "SDK is not defined"
                }
            })

            addToRight(NonOpaquePanel(HorizontalLayout(scale(12))).apply {
                add(createConfigureAction())
                add(createCloseAction())
            })
        }

        override fun getBackground(): Color? =
            EditorColorsManager.getInstance().globalScheme.getColor(HintUtil.ERROR_COLOR_KEY) ?: super.getBackground()

        private fun createConfigureAction(): JComponent =
            HyperlinkLabel("Setup SDK").apply {
                addHyperlinkListener {
                    val projectStructure = ProjectStructureConfigurable.getInstance(model.project)
                    val isEdited = ShowSettingsUtil.getInstance().editConfigurable(model.project, projectStructure)
                    { projectStructure.select(model.srcModule.name, ClasspathEditor.getName(), true) }

                    val sdkVersion = findSdkVersion(model.srcModule)
                    val sdkFixed = isEdited && sdkVersion.feature in minSupportedSdkVersion..maxSupportedSdkVersion
                    if (sdkFixed) {
                        this@SdkNotificationPanel.isVisible = false
                        isOKActionEnabled = true
                    }
                }
            }

        private fun createCloseAction(): JComponent =
            InplaceButton(IconButton(null, AllIcons.Actions.Close, AllIcons.Actions.CloseHovered)) {
                this@SdkNotificationPanel.isVisible = false
            }
    }

    private fun updateMembersTable() {
        val srcClasses = model.srcClasses

        val items = if (model.extractMembersFromSrcClasses) {
            srcClasses.flatMap { it.extractFirstLevelMembers(false) }
        } else {
            srcClasses.map { MemberInfo(it) }
        }.toMutableList().sortedWith { o1, o2 -> o1.displayName.compareTo(o2.displayName, true) }

        checkMembers(items)
        membersTable.setMemberInfos(items)
        if (items.isEmpty()) isOKActionEnabled = false

        // Fix issue with MemberSelectionTable height, set it directly.
        // Use row height times methods (12 max) plus one more for header
        val height = membersTable.rowHeight * (items.size.coerceAtMost(12) + 1)
        membersTable.preferredScrollableViewportSize = size(-1, height)
    }

    private fun checkMembers(allMembers: Collection<MemberInfo>) {
        val selectedDisplayNames = model.selectedMembers.map { it.displayName }
        val selectedMembers = allMembers.filter { it.displayName in selectedDisplayNames }

        val methodsToCheck = selectedMembers.ifEmpty { allMembers }
        methodsToCheck.forEach { it.isChecked = true }
    }

    private fun getTestRoot() : VirtualFile? {
        model.testSourceRoot?.let {
            if (it.isDirectory || it is FakeVirtualFile) return it
        }
        return null
    }

    private fun VirtualFile.toRealFile():VirtualFile = if (this is FakeVirtualFile)  this.parent else this

    override fun doValidate(): ValidationInfo? {
        val testRoot = getTestRoot()
            ?: return ValidationInfo("Test source root is not configured", testSourceFolderField.childComponent)

        if (!model.project.isBuildWithGradle && ModuleUtil.findModuleForFile(testRoot.toRealFile(), model.project) == null) {
            return ValidationInfo("Test source root is located out of any module", testSourceFolderField.childComponent)
        }

        membersTable.tableHeader?.background = UIUtil.getTableBackground()
        membersTable.background = UIUtil.getTableBackground()
        if (membersTable.selectedMemberInfos.isEmpty()) {
            membersTable.tableHeader?.background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
            membersTable.background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
            return ValidationInfo(
                "Tick any methods to generate tests for", membersTable
            )
        }
        return null
    }

    class OKOptionAction(val testsModel: GenerateTestsModel, val okAction : Action) : AbstractAction(testsModel.getActionText()), OptionAction {
        init {
            putValue(DEFAULT_ACTION, java.lang.Boolean.TRUE)
            putValue(FOCUSED_ACTION, java.lang.Boolean.TRUE)
        }
        private val generateAction = object : AbstractAction(ACTION_GENERATE) {
            override fun actionPerformed(e: ActionEvent?) {
                testsModel.runGeneratedTestsWithCoverage = false
                updateButtonText(e)
            }
        }
        private val generateAndRunAction = object : AbstractAction(ACTION_GENERATE_AND_RUN) {
            override fun actionPerformed(e: ActionEvent?) {
                testsModel.runGeneratedTestsWithCoverage = true
                updateButtonText(e)
            }
        }

        private fun updateButtonText(e: ActionEvent?) {
            with(e?.source as JButton) {
                text = testsModel.getActionText()
                testsModel.project.service<Settings>().runGeneratedTestsWithCoverage =
                    testsModel.runGeneratedTestsWithCoverage
                repaint()
            }
        }

        override fun actionPerformed(e: ActionEvent?) {
            okAction.actionPerformed(e)
        }

        override fun getOptions(): Array<Action> {
            if (testsModel.runGeneratedTestsWithCoverage) return arrayOf(generateAndRunAction, generateAction)
            return arrayOf(generateAction, generateAndRunAction)
        }
    }

    private val okOptionAction: OKOptionAction get() = OKOptionAction(model, super.getOKAction())
    override fun getOKAction() = okOptionAction

    override fun doOKAction() {
        fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))

        logger.info { "Tests generation instantiation phase started at ${now()}" }

        model.testPackageName =
            if (testPackageField.text != SAME_PACKAGE_LABEL) testPackageField.text else ""

        val selectedMembers = membersTable.selectedMemberInfos
        if (!model.extractMembersFromSrcClasses) {
            model.srcClasses = selectedMembers
                .mapNotNull { it.member as? PsiClass }
                .toSet()
        }
        model.selectedMembers = selectedMembers.toSet()

        model.testFramework = testFrameworks.item
        model.mockStrategy = mockStrategies.item
        model.parametrizedTestSource =
            if (parametrizedTestSources.isSelected) ParametrizedTestSource.PARAMETRIZE else ParametrizedTestSource.DO_NOT_PARAMETRIZE

        model.mockFramework = MOCKITO
        model.staticsMocking = if (staticsMocking.isSelected) MockitoStaticMocking else NoStaticMocking
        try {
            timeoutSpinner.commitEdit()
        } catch (ignored: ParseException) {
        }
        model.timeout = TimeUnit.SECONDS.toMillis(timeoutSpinner.number.toLong())
        model.testSourceRoot?.apply { model.updateSourceRootHistory(this.toNioPath().toString()) }

        model.typeReplacementApproach =
            when (springConfig.item.getItem()) {
                NO_SPRING_CONFIGURATION_OPTION -> TypeReplacementApproach.DoNotReplace
                else -> TypeReplacementApproach.ReplaceIfPossible(springConfig.item.getItem().toString())
            }

        val settings = model.project.service<Settings>()
        with(settings) {
            model.runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour
            model.hangingTestsTimeout = hangingTestsTimeout
            model.runInspectionAfterTestGeneration = runInspectionAfterTestGeneration
            model.forceStaticMocking = forceStaticMocking
            model.chosenClassesToMockAlways = chosenClassesToMockAlways()
            model.fuzzingValue = fuzzingValue
            model.commentStyle = javaDocCommentStyle
            model.summariesGenerationType = state.summariesGenerationType
            UtSettings.treatOverflowAsError = treatOverflowAsError == TreatOverflowAsError.AS_ERROR
        }

        // Firstly, save settings
        loadStateFromModel(settings, model)
        // Then, process force static mocking case
        model.generateWarningsForStaticMocking = model.staticsMocking is NoStaticMocking
        if (model.forceStaticMocking == ForceStaticMocking.FORCE) {
            // We need mock framework extension to mock statics, no user provided => choose default
            if (model.staticsMocking is NoStaticMocking) {
                model.staticsMocking = StaticsMocking.defaultItem
            }
        }

        try {
            val testRootPrepared = createTestRootAndPackages()
            if (!testRootPrepared) {
                showTestRootAbsenceErrorMessage()
                return
            }
        } catch (e: IncorrectOperationException) {
            println(e.message)
        }

        configureJvmTargetIfRequired()
        configureTestFrameworkIfRequired()
        configureMockFrameworkIfRequired()
        configureStaticMockingIfRequired()
        configureParametrizedTestsIfRequired()

        logger.info { "Tests generation instantiation phase finished at ${now()}" }
        super.doOKAction()
    }

    /**
     * Creates test source root if absent and target packages for tests.
     */
    private fun createTestRootAndPackages(): Boolean {
        model.setSourceRootAndFindTestModule(createDirectoryIfMissing(model.testSourceRoot))
        val testSourceRoot = model.testSourceRoot ?: return false

        if (model.testSourceRoot?.isDirectory != true) return false
        if (getOrCreateTestRoot(testSourceRoot)) {
            if (cbSpecifyTestPackage.isSelected) {
                createSelectedPackage(testSourceRoot)
            } else {
                createPackagesByClasses(testSourceRoot)
            }
            return true
        }
        return false
    }

    private fun createDirectoryIfMissing(dir : VirtualFile?): VirtualFile? {
        val file = if (dir is FakeVirtualFile) {
            WriteCommandAction.runWriteCommandAction(model.project, Computable<VirtualFile> {
                VfsUtil.createDirectoryIfMissing(dir.path)
            })
        } else {
            dir
        }?: return null
        return if (VfsUtil.virtualToIoFile(file).isFile) {
            null
        } else {
            StandardFileSystems.local().findFileByPath(file.path)
        }
    }

    private fun createPackagesByClasses(testSourceRoot: VirtualFile) {
        val packageNames = model.srcClasses.map { it.packageName }.sortedBy { it.length }
        for (packageName in packageNames) {
            runWriteAction {
                RefactoringUtil.createPackageDirectoryInSourceRoot(createPackageWrapper(packageName), testSourceRoot)
            }
        }
    }

    private fun createSelectedPackage(testSourceRoot: VirtualFile) =
        runWriteAction {
            RefactoringUtil.createPackageDirectoryInSourceRoot(createPackageWrapper(testPackageField.text), testSourceRoot)
        }

    private fun showTestRootAbsenceErrorMessage() =
        Messages.showErrorDialog(
            "Test source root is not configured or is located out of content entry!",
            "Generation Error"
        )

    private fun getOrCreateTestRoot(testSourceRoot: VirtualFile): Boolean {
        val modifiableModel = ModuleRootManager.getInstance(model.testModule).modifiableModel
        try {
            val contentEntry = modifiableModel.contentEntries
                .filterNot { it.file == null }
                .firstOrNull { VfsUtil.isAncestor(it.file!!, testSourceRoot, false) }
                ?: return false

            contentEntry.addSourceRootIfAbsent(
                modifiableModel,
                testSourceRoot.url,
                codegenLanguages.item.testRootType()
            )
            return true
        } finally {
            if (modifiableModel.isWritable && !modifiableModel.isDisposed) modifiableModel.dispose()
        }
    }

    private fun createPackageWrapper(packageName: String?): PackageWrapper =
        PackageWrapper(PsiManager.getInstance(model.project), trimPackageName(packageName))

    private fun trimPackageName(name: String?): String = name?.trim() ?: ""

    private fun initDefaultValues() {
        testPackageField.isEnabled = false
        cbSpecifyTestPackage.isEnabled = model.srcClasses.all { cl -> cl.packageName.isNotEmpty() }

        val settings = model.project.service<Settings>()

        mockStrategies.item = when (model.projectType) {
            ProjectType.Spring -> MockStrategyApi.springDefaultItem
            else ->    settings.mockStrategy
        }
        staticsMocking.isSelected = settings.staticsMocking == MockitoStaticMocking
        parametrizedTestSources.isSelected = settings.parametrizedTestSource == ParametrizedTestSource.PARAMETRIZE

        mockStrategies.isEnabled = true
        staticsMocking.isEnabled = mockStrategies.item != MockStrategyApi.NO_MOCKS

        codegenLanguages.item = model.codegenLanguage

        val installedTestFramework = TestFramework.allItems.singleOrNull { it.isInstalled }
        currentFrameworkItem = when (parametrizedTestSources.isSelected) {
            false -> installedTestFramework ?: settings.testFramework
            true -> installedTestFramework
                ?: if (settings.testFramework != Junit4) settings.testFramework else TestFramework.parametrizedDefaultItem
        }

        updateTestFrameworksList(settings.parametrizedTestSource)
        updateParametrizationEnabled()

        updateSpringConfigurationEnabled()

        updateMockStrategyList()

        itemsToHelpTooltip.forEach { (box, tooltip) ->
            if (tooltip != null && box is ComboBox<*>) {
                val item = box.item
                if (item is CodeGenerationSettingItem) {
                    tooltip.toolTipText = item.description
                }
            }
        }
    }

    /**
     * This region configures frameworks if required.
     *
     * We need to notify the user about potential problems and to give
     * him a chance to install missing frameworks into his application.
     */
    private fun configureTestFrameworkIfRequired() {
        val testFramework = testFrameworks.item
        if (!testFramework.isInstalled) {
            configureTestFramework()

            // Configuring framework will configure parametrized tests automatically
            // TODO: do something more general here
            // Note: we can't just update isParametrizedTestsConfigured as before because project.allLibraries() won't be updated immediately
            testFramework.isParametrizedTestsConfigured = true
        }

        model.conflictTriggers[Conflict.TestFrameworkConflict] = TestFramework.allItems.count { it.isInstalled  } > 1
    }

    private fun configureMockFrameworkIfRequired() {
        if (mockStrategies.item != MockStrategyApi.NO_MOCKS && !MOCKITO.isInstalled) {
            configureMockFramework()
        }
    }

    private fun configureStaticMockingIfRequired() {
        if (staticsMocking.isSelected && !MockitoStaticMocking.isConfigured) {
            configureStaticMocking()
        }
    }

    private fun configureParametrizedTestsIfRequired() {
        if (parametrizedTestSources.isSelected && !testFrameworks.item.isParametrizedTestsConfigured) {
            configureParametrizedTests()
        }
    }

    private fun configureTestFramework() {
        val selectedTestFramework = testFrameworks.item

        val libraryInProject =
            findFrameworkLibrary(model.project, model.testModule, selectedTestFramework, LibrarySearchScope.Project)
        val versionInProject = libraryInProject?.libraryName?.parseVersion()
        val sdkVersion = findSdkVersion(model.srcModule).feature

        val libraryDescriptor = when (selectedTestFramework) {
            Junit4 -> jUnit4LibraryDescriptor(versionInProject)
            Junit5 -> jUnit5LibraryDescriptor(versionInProject)
            TestNg -> when (sdkVersion) {
                minSupportedSdkVersion -> testNgOldLibraryDescriptor()
                else -> testNgNewLibraryDescriptor(versionInProject)
            }
             else -> throw UnsupportedOperationException()
        }

        selectedTestFramework.isInstalled = true
        addDependency(model.testModule, libraryDescriptor)
            .onError { selectedTestFramework.isInstalled = false }
    }

    private fun configureMockFramework() {
        val selectedMockFramework = MOCKITO

        val libraryInProject =
            findFrameworkLibrary(model.project, model.testModule, selectedMockFramework, LibrarySearchScope.Project)
        val versionInProject = libraryInProject?.libraryName?.parseVersion()

        selectedMockFramework.isInstalled = true
        addDependency(model.testModule, mockitoCoreLibraryDescriptor(versionInProject))
            .onError { selectedMockFramework.isInstalled = false }
    }

    private fun configureStaticMocking() {
        val testResourcesUrl = model.testModule.getOrCreateTestResourcesPath(model.testSourceRoot)
        configureMockitoResources(testResourcesUrl)

        MockitoStaticMocking.isConfigured = true
    }

    /**
     * Configures Mockito-core to use an experimental feature for static mocking.
     * Returns true if mockito resource have already been configured before.
     *
     * See https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#39
     * for further details.
     */
    private fun configureMockitoResources(testResourcesPath: Path) {
        val mockitoExtensionsPath = "$testResourcesPath/$MOCKITO_EXTENSIONS_FOLDER".toPath()
        val mockitoMockMakerPath = "$mockitoExtensionsPath/$MOCKITO_MOCKMAKER_FILE_NAME".toPath()

        if (!testResourcesPath.exists()) Files.createDirectory(testResourcesPath)
        if (!mockitoExtensionsPath.exists()) Files.createDirectory(mockitoExtensionsPath)

        if (!mockitoMockMakerPath.exists()) {
            Files.createFile(mockitoMockMakerPath)
            Files.write(mockitoMockMakerPath, listOf(MOCKITO_EXTENSIONS_FILE_CONTENT))
        }
    }

    private fun configureParametrizedTests() {
        // TODO: currently first three declarations are copy-pasted from configureTestFramework(), maybe fix this somehow?
        val selectedTestFramework = testFrameworks.item

        val libraryInProject =
            findFrameworkLibrary(model.project, model.testModule, selectedTestFramework, LibrarySearchScope.Project)
        val versionInProject = libraryInProject?.libraryName?.parseVersion()

        val libraryDescriptor: ExternalLibraryDescriptor? = when (selectedTestFramework) {
            Junit4 -> error("Parametrized tests are not supported for JUnit 4")
            Junit5 -> jUnit5ParametrizedTestsLibraryDescriptor(versionInProject)
            TestNg -> null // Parametrized tests come with TestNG by default
            else -> throw UnsupportedOperationException()
        }

        selectedTestFramework.isParametrizedTestsConfigured = true
        libraryDescriptor?.let {
            addDependency(model.testModule, it)
                .onError { selectedTestFramework.isParametrizedTestsConfigured = false }
        }
    }

    /**
     * Adds the dependency for selected framework via [JavaProjectModelModificationService].
     *
     * Note that version restrictions will be applied only if they are present on target machine
     * Otherwise latest release version will be installed.
     */
    private fun addDependency(module: Module, libraryDescriptor: ExternalLibraryDescriptor): Promise<Void> {
        val promise = JavaProjectModelModificationService
            .getInstance(model.project)
            //this method returns JetBrains internal Promise that is difficult to deal with, but it is our way
            .addDependency(model.testModule, libraryDescriptor, DependencyScope.TEST)
        promise.thenRun {
            module.allLibraries()
                .lastOrNull { library -> library.presentableName.contains(libraryDescriptor.id) }?.let {
                    ModuleRootModificationUtil.updateModel(module) { model -> placeEntryToCorrectPlace(model, it) }
                }
        }
        return promise
    }

    /**
     * Reorders library list to unsure that just added library with proper version is listed prior to old-versioned one
     */
    private fun placeEntryToCorrectPlace(model: ModifiableRootModel, addedEntry: LibraryOrderEntry) {
        val order = model.orderEntries
        val lastEntry = order.last()
        if (lastEntry is LibraryOrderEntry && lastEntry.library == addedEntry.library) {
            val insertionPoint = order.indexOfFirst { it is ModuleSourceOrderEntry } + 1
            if (insertionPoint > 0) {
                System.arraycopy(order, insertionPoint, order, insertionPoint + 1, order.size - 1 - insertionPoint)
                order[insertionPoint] = lastEntry
                model.rearrangeOrderEntries(order)
            }
        }
    }

    //endregion

    /**
     * Configures JVM Target if required.
     *
     * We need to notify the user about potential problems and to give
     * him a chance to change JVM targets in his application.
     *
     * Note that now we need it for Kotlin plugin only.
     */
    private fun configureJvmTargetIfRequired() {
        if (codegenLanguages.item == CodegenLanguage.KOTLIN
            && parametrizedTestSources.isSelected
            && createKotlinJvmTargetNotificationDialog() == Messages.YES
        ) {
            configureKotlinJvmTarget()
        }
    }

    /**
     * Checks if JVM target for Kotlin plugin if configured appropriately
     * and allows user to configure it via ProjectStructure tab if not.
     *
     * For Kotlin plugin until version 1.5 default JVM target is 1.6.
     * Sometimes (i.e. in parametrized tests) we use some annotations
     * and statements that are supported since JVM version 1.8 only.
     */
    private fun configureKotlinJvmTarget() {
        val activeKotlinJvmTarget = model.srcModule.kotlinTargetPlatform().description
        if (activeKotlinJvmTarget == actualKotlinJvmTarget) {
            return
        }

        ShowSettingsUtil.getInstance().editConfigurable(
            model.project,
            ProjectStructureConfigurable.getInstance(Objects.requireNonNull(model.project))
        )
    }

    private fun createKotlinJvmTargetNotificationDialog() = Messages.showYesNoDialog(
        """Your current JVM target is 1.6. Some Kotlin features may not be supported. 
            |Would you like to update current target to $actualKotlinJvmTarget?""".trimMargin(),
        title,
        "Yes",
        "No",
        Messages.getQuestionIcon(),
        ProjectNewWindowDoNotAskOption(),
    )

    // Language features we use to generate parametrized tests
    // (i.e. @JvmStatic attribute or JUnit5 arguments) are supported since JVM target 1.8
    private val actualKotlinJvmTarget = "1.8"

    private fun setListeners() {
        itemsToHelpTooltip.forEach { (box, tooltip) -> if (box is ComboBox<*> && tooltip != null) {
            box.setHelpTooltipTextChanger(tooltip)
        } }

        testSourceFolderField.childComponent.addActionListener { event ->
            with((event.source as JComboBox<*>).selectedItem) {
                if (this is VirtualFile) {
                    model.setSourceRootAndFindTestModule(this@with)
                }
                else {
                    model.setSourceRootAndFindTestModule(null)
                }
            }
        }

        mockStrategies.addActionListener { event ->
            val comboBox = event.source as ComboBox<*>
            val item = comboBox.item as MockStrategyApi

            staticsMocking.isEnabled = item != MockStrategyApi.NO_MOCKS
            if (!staticsMocking.isEnabled) {
                staticsMocking.isSelected = false
            }

            updateParametrizationEnabled()
        }

        testFrameworks.addActionListener { event ->
            val comboBox = event.source as ComboBox<*>
            val item = comboBox.item as TestFramework

            currentFrameworkItem = item

            updateParametrizationEnabled()
        }

        codegenLanguages.addActionListener { _ ->
            updateParametrizationEnabled()
        }

        parametrizedTestSources.addActionListener { _ ->
            val parametrizedTestSource = if (parametrizedTestSources.isSelected) {
                ParametrizedTestSource.PARAMETRIZE
            } else {
                ParametrizedTestSource.DO_NOT_PARAMETRIZE
            }

            when (parametrizedTestSource) {
                ParametrizedTestSource.PARAMETRIZE -> {
                    mockStrategies.item = MockStrategyApi.NO_MOCKS
                    staticsMocking.isEnabled = false
                    staticsMocking.isSelected = false
                }
                ParametrizedTestSource.DO_NOT_PARAMETRIZE -> {
                    mockStrategies.isEnabled = true
                    if (mockStrategies.item != MockStrategyApi.NO_MOCKS) {
                        staticsMocking.isEnabled = true
                        staticsMocking.isSelected = true
                    }
                }
            }

            updateTestFrameworksList(parametrizedTestSource)
        }

        springConfig.addActionListener { _ ->
            val isSpringConfigSelected = springConfig.item.getItem() != NO_SPRING_CONFIGURATION_OPTION
            if (isSpringConfigSelected) {
                mockStrategies.item = MockStrategyApi.springDefaultItem
                mockStrategies.isEnabled = false
                updateMockStrategyListForConfigGuidedTypeReplacements()
            } else {
                mockStrategies.item = when (model.projectType) {
                    ProjectType.Spring -> MockStrategyApi.springDefaultItem
                    else -> MockStrategyApi.defaultItem
                }
                mockStrategies.isEnabled = true
                updateMockStrategyList()
            }
        }

        cbSpecifyTestPackage.addActionListener {
            val testPackageName = findTestPackageComboValue()
            val packageNameIsNeeded = testPackageField.isEnabled || testPackageName != SAME_PACKAGE_LABEL

            testPackageField.text = if (packageNameIsNeeded) testPackageName else ""
            testPackageField.isEnabled = !testPackageField.isEnabled
        }
    }

    private lateinit var currentFrameworkItem: TestFramework

    // We would like to remove JUnit4 from framework list in parametrized mode
    private fun updateTestFrameworksList(parametrizedTestSource: ParametrizedTestSource) {
        // We do not support parameterized tests for JUnit4
        val enabledTestFrameworks = when (parametrizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> TestFramework.allItems
            ParametrizedTestSource.PARAMETRIZE -> TestFramework.allItems.filterNot { it == Junit4 }
        }

        var defaultItem = when (parametrizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> TestFramework.defaultItem
            ParametrizedTestSource.PARAMETRIZE -> TestFramework.parametrizedDefaultItem
        }
        enabledTestFrameworks.forEach { if (it.isInstalled && !defaultItem.isInstalled) defaultItem = it }

        testFrameworks.model = DefaultComboBoxModel(enabledTestFrameworks.toTypedArray())
        testFrameworks.item = if (currentFrameworkItem in enabledTestFrameworks) currentFrameworkItem else defaultItem
        testFrameworks.renderer = createTestFrameworksRenderer(WILL_BE_INSTALLED_LABEL)

        currentFrameworkItem = testFrameworks.item
    }

    private fun updateParametrizationEnabled() {
        val languageIsSupported = codegenLanguages.item == CodegenLanguage.JAVA
        val frameworkIsSupported = currentFrameworkItem == Junit5
                || currentFrameworkItem == TestNg && findSdkVersion(model.srcModule).feature > minSupportedSdkVersion
        val mockStrategyIsSupported = mockStrategies.item == MockStrategyApi.NO_MOCKS

        // We do not support PUT in Spring projects
        val isSupportedProjectType = model.projectType == ProjectType.PureJvm
        parametrizedTestSources.isEnabled =
            isSupportedProjectType && languageIsSupported && frameworkIsSupported && mockStrategyIsSupported

        if (!parametrizedTestSources.isEnabled) {
            parametrizedTestSources.isSelected = false
        }
    }

    private fun updateMockStrategyList() {
        mockStrategies.renderer = object : ColoredListCellRenderer<MockStrategyApi>() {
            override fun customizeCellRenderer(
                list: JList<out MockStrategyApi>, value: MockStrategyApi,
                index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                this.append(value.displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                if (value != MockStrategyApi.NO_MOCKS && !MOCKITO.isInstalled) {
                    this.append(WILL_BE_INSTALLED_LABEL, SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
            }
        }
    }

    private fun updateMockStrategyListForConfigGuidedTypeReplacements() {
        mockStrategies.renderer = object : ColoredListCellRenderer<MockStrategyApi>() {
            override fun customizeCellRenderer(
                list: JList<out MockStrategyApi>, value: MockStrategyApi,
                index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                this.append("Mock using Spring configuration", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }

    private fun updateSpringConfigurationEnabled() {
        // We check for > 1 because there is already extra-dummy NO_SPRING_CONFIGURATION_OPTION option
        springConfig.isEnabled = model.projectType == ProjectType.Spring
                && modelSpringConfigs.size > 1
    }

    private fun staticsMockingConfigured(): Boolean {
        val entries = ModuleRootManager.getInstance(model.testModule).contentEntries
        val hasEntriesWithoutResources = entries
            .filterNot { it.sourceFolders.any { f -> f.rootType in testResourceRootTypes } }
            .isNotEmpty()

        if (hasEntriesWithoutResources) {
            return false
        }

        val entriesPaths = entries
            .flatMap {
                it.sourceFolders
                    .filter { f -> f.rootType in testResourceRootTypes }
                    .map { f -> Paths.get(urlToPath(f.url)) }
            }

        return entriesPaths.all { entryPath ->
                if (!Files.exists(entryPath)) return false

                val mockMakerPath = "$entryPath/$MOCKITO_EXTENSIONS_FOLDER/$MOCKITO_MOCKMAKER_FILE_NAME".toPath()
                if (!Files.exists(mockMakerPath)) return false

                try {
                    val fileLines = Files.readAllLines(mockMakerPath)
                    fileLines.singleOrNull() == MOCKITO_EXTENSIONS_FILE_CONTENT
                } catch (e: java.io.IOException) {
                    return false
                }

            }
    }
}

fun GenerateTestsModel.getActionText() : String =
    if (this.runGeneratedTestsWithCoverage) ACTION_GENERATE_AND_RUN else ACTION_GENERATE

private fun ComboBox<*>.setHelpTooltipTextChanger(helpLabel: JBLabel) {
    addActionListener { event ->
        val comboBox = event.source as ComboBox<*>
        val item = comboBox.item
        if (item is CodeGenerationSettingItem) {
            helpLabel.toolTipText = item.description
        }
    }
}
