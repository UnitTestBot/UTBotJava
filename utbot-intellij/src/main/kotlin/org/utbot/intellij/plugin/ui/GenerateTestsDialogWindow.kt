@file:Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")

package org.utbot.intellij.plugin.ui

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.icons.AllIcons
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.util.MOCKITO_EXTENSIONS_FILE_CONTENT
import org.utbot.framework.codegen.model.util.MOCKITO_EXTENSIONS_STORAGE
import org.utbot.framework.codegen.model.util.MOCKITO_MOCKMAKER_FILE_NAME
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockFramework.MOCKITO
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import org.utbot.intellij.plugin.ui.utils.LibrarySearchScope
import org.utbot.intellij.plugin.ui.utils.findFrameworkLibrary
import org.utbot.intellij.plugin.ui.utils.getOrCreateTestResourcesPath
import org.utbot.intellij.plugin.ui.utils.kotlinTargetPlatform
import org.utbot.intellij.plugin.ui.utils.parseVersion
import org.utbot.intellij.plugin.ui.utils.testResourceRootTypes
import org.utbot.intellij.plugin.ui.utils.addSourceRootIfAbsent
import org.utbot.intellij.plugin.ui.utils.testRootType
import com.intellij.ide.impl.ProjectNewWindowDoNotAskOption
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModificationService
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore.urlToPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.ui.MemberSelectionTable
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
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
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.IncorrectOperationException
import com.intellij.util.io.exists
import com.intellij.util.lang.JavaVersion
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Borders.merge
import com.intellij.util.ui.JBUI.scale
import com.intellij.util.ui.JBUI.size
import com.intellij.util.ui.UIUtil
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.models.jUnit4LibraryDescriptor
import org.utbot.intellij.plugin.models.jUnit5LibraryDescriptor
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.models.testNgLibraryDescriptor
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.concurrency.Promise
import org.utbot.intellij.plugin.models.mockitoCoreLibraryDescriptor
import org.utbot.intellij.plugin.util.AndroidApiHelper
import java.awt.BorderLayout
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Objects
import java.util.concurrent.TimeUnit
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import kotlin.streams.toList
import org.jetbrains.concurrency.thenRun
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.utbot.intellij.plugin.ui.utils.allLibraries

private const val RECENTS_KEY = "org.utbot.recents"

private const val SAME_PACKAGE_LABEL = "same as for sources"

private const val WILL_BE_INSTALLED_LABEL = " (will be installed)"
private const val WILL_BE_CONFIGURED_LABEL = " (will be configured)"
private const val MINIMUM_TIMEOUT_VALUE_IN_SECONDS = 1

class GenerateTestsDialogWindow(val model: GenerateTestsModel) : DialogWrapper(model.project) {
    companion object {
        const val minSupportedSdkVersion = 8
        const val maxSupportedSdkVersion = 11
    }

    private val membersTable = MemberSelectionTable(emptyList(), null)

    private val cbSpecifyTestPackage = CheckBox("Specify destination package", false)
    private val testPackageField = PackageNameReferenceEditorCombo(
        findTestPackageComboValue(),
        model.project,
        RECENTS_KEY,
        "Choose destination package"
    )

    private val testSourceFolderField = TestFolderComboWithBrowseButton(model)

    private val codegenLanguages = ComboBox(DefaultComboBoxModel(CodegenLanguage.values()))
    private val testFrameworks = ComboBox(DefaultComboBoxModel(TestFramework.allItems.toTypedArray()))
    private val mockStrategies = ComboBox(DefaultComboBoxModel(MockStrategyApi.values()))
    private val staticsMocking = ComboBox(DefaultComboBoxModel(StaticsMocking.allItems.toTypedArray()))
    private val timeoutSpinner =
        JBIntSpinner(
            TimeUnit.MILLISECONDS.toSeconds(UtSettings.utBotGenerationTimeoutInMillis).toInt(),
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS,
            Int.MAX_VALUE,
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS
        )
    private val parametrizedTestSources = ComboBox(DefaultComboBoxModel(ParametrizedTestSource.values()))

    private lateinit var mockExtensionRow: Row
    private lateinit var panel: DialogPanel

    @Suppress("UNCHECKED_CAST")
    private val itemsToHelpTooltip = hashMapOf(
        (codegenLanguages as ComboBox<CodeGenerationSettingItem>) to createHelpLabel(),
        (testFrameworks as ComboBox<CodeGenerationSettingItem>) to createHelpLabel(),
        (mockStrategies as ComboBox<CodeGenerationSettingItem>) to createHelpLabel(),
        (staticsMocking as ComboBox<CodeGenerationSettingItem>) to createHelpLabel(),
        (parametrizedTestSources as ComboBox<CodeGenerationSettingItem>) to createHelpLabel()
    )

    private fun createHelpLabel() = JBLabel(AllIcons.General.ContextHelp)

    init {
        title = "Generate tests with UtBot"
        setResizable(false)

        TestFramework.allItems.forEach {
            it.isInstalled = findFrameworkLibrary(model.project, model.testModule, it) != null
        }
        MockFramework.allItems.forEach {
            it.isInstalled = findFrameworkLibrary(model.project, model.testModule, it) != null
        }
        StaticsMocking.allItems.forEach { it.isConfigured = staticsMockingConfigured() }


        // Configure notification urls callbacks
        TestsReportNotifier.urlOpeningListener.callbacks[TestReportUrlOpeningListener.mockitoSuffix]?.plusAssign {
            if (createMockFrameworkNotificationDialog() == Messages.YES) {
                configureMockFramework()
            }
        }

        TestsReportNotifier.urlOpeningListener.callbacks[TestReportUrlOpeningListener.mockitoInlineSuffix]?.plusAssign {
            if (createStaticsMockingNotificationDialog() == Messages.YES) {
                configureStaticMocking()
            }
        }

        init()
    }


    @Suppress("UNCHECKED_CAST")
    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                component(testSourceFolderField)
            }
            row("Code generation language:") {
                makePanelWithHelpTooltip(
                    codegenLanguages as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[codegenLanguages]
                )
            }
            row("Test framework:") {
                makePanelWithHelpTooltip(
                    testFrameworks as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[testFrameworks]
                )
            }
            row("Mock strategy:") {
                makePanelWithHelpTooltip(
                    mockStrategies as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[mockStrategies]
                )
            }
            mockExtensionRow = row("Mock static:") {
                makePanelWithHelpTooltip(
                    staticsMocking as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[staticsMocking]
                )
            }
            row("Timeout for class:") {
                panelWithHelpTooltip("The execution timeout specifies time for symbolic and concrete analysis") {
                    component(timeoutSpinner)
                    component(JBLabel("sec"))
                }

            }
            row("Parametrized test:") {
                makePanelWithHelpTooltip(
                    parametrizedTestSources as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[parametrizedTestSources]
                )
            }
            row {
                component(cbSpecifyTestPackage)
            }.apply { visible = false }
            row("Destination package:") {
                component(testPackageField)
            }.apply { visible = false }

            row("Generate test methods for:") {}
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

    private fun findSdkVersion(): JavaVersion? {
        val projectSdk = ModuleRootManager.getInstance(model.srcModule).sdk
        return JavaVersion.tryParse(projectSdk?.versionString)
    }

    override fun createTitlePane(): JComponent? {
        val sdkVersion = findSdkVersion()
        //TODO:SAT-1571 investigate Android Studio specific sdk issues
        if (sdkVersion?.feature in minSupportedSdkVersion..maxSupportedSdkVersion || AndroidApiHelper.isAndroidStudio()) return null
        isOKActionEnabled = false
        return SdkNotificationPanel(model, sdkVersion)
    }

    private fun findTestPackageComboValue(): String {
        val packageNames = model.srcClasses.map { it.packageName }.distinct()
        return if (packageNames.size == 1) packageNames.first() else SAME_PACKAGE_LABEL
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
                    "SDK version $sdkVersion is not supported, use ${JavaSdkVersion.JDK_1_8} or ${JavaSdkVersion.JDK_11}."
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

                    val sdkVersion = findSdkVersion()
                    val sdkFixed = isEdited && sdkVersion?.feature in minSupportedSdkVersion..maxSupportedSdkVersion
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

        val items: List<MemberInfo>
        if (srcClasses.size == 1) {
            items = TestIntegrationUtils.extractClassMethods(srcClasses.single(), false)
            updateMethodsTable(items)
        } else {
            items = srcClasses.map { MemberInfo(it) }
            updateClassesTable(items)
        }

        if (items.isEmpty()) isOKActionEnabled = false

        // fix issue with MemberSelectionTable height, set it directly.
        // Use row height times methods (12 max) plus one more for header
        val height = membersTable.rowHeight * (items.size.coerceAtMost(12) + 1)
        membersTable.preferredScrollableViewportSize = size(-1, height)
    }

    private fun updateMethodsTable(allMethods: List<MemberInfo>) {
        val selectedDisplayNames = model.selectedMethods?.map { it.displayName } ?: emptyList()
        val selectedMethods = if (selectedDisplayNames.isEmpty())
            allMethods
            else allMethods.filter { it.displayName in selectedDisplayNames }

        if (selectedMethods.isEmpty()) {
            checkMembers(allMethods)
        } else {
            checkMembers(selectedMethods)
        }

        membersTable.setMemberInfos(allMethods)
    }

    private fun updateClassesTable(srcClasses: List<MemberInfo>) {
        checkMembers(srcClasses)
        membersTable.setMemberInfos(srcClasses)
    }

    private fun checkMembers(members: List<MemberInfo>) = members.forEach { it.isChecked = true }

    private fun getTestRoot() : VirtualFile? {
        model.testSourceRoot?.let {
            if (it.isDirectory || it is FakeVirtualFile) return it
        }
        return null
    }

    override fun doValidate(): ValidationInfo? {
        val testRoot = getTestRoot()
            ?: return ValidationInfo("Test source root is not configured", testSourceFolderField.childComponent)

        if (findReadOnlyContentEntry(testRoot) == null) {
            return ValidationInfo("Test source root is located out of content entry", testSourceFolderField.childComponent)
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


    override fun doOKAction() {
        model.testPackageName =
            if (testPackageField.text != SAME_PACKAGE_LABEL) testPackageField.text else ""

        val selectedMembers = membersTable.selectedMemberInfos
        model.srcClasses = selectedMembers
            .mapNotNull { it.member as? PsiClass ?: it.member.containingClass }
            .toSet()

        val selectedMethods = selectedMembers.filter { it.member is PsiMethod }.toSet()
        model.selectedMethods = if (selectedMethods.any()) selectedMethods else null

        model.testFramework = testFrameworks.item
        model.mockStrategy = mockStrategies.item
        model.parametrizedTestSource = parametrizedTestSources.item

        model.mockFramework = MOCKITO
        model.staticsMocking = staticsMocking.item
        model.codegenLanguage = codegenLanguages.item
        model.timeout = TimeUnit.SECONDS.toMillis(timeoutSpinner.number.toLong())

        val settings = model.project.service<Settings>()
        with(settings) {
            model.runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour
            model.hangingTestsTimeout = hangingTestsTimeout
            model.forceStaticMocking = forceStaticMocking
            model.chosenClassesToMockAlways = chosenClassesToMockAlways()
            UtSettings.treatOverflowAsError = treatOverflowAsError == TreatOverflowAsError.AS_ERROR
        }

        // firstly save settings
        settings.loadStateFromModel(model)
        // then process force static mocking case
        model.generateWarningsForStaticMocking = model.staticsMocking is NoStaticMocking
        if (model.forceStaticMocking == ForceStaticMocking.FORCE) {
            // we have to use mock framework to mock statics, no user provided => choose default
            if (model.mockFramework == null) {
                model.mockFramework = MockFramework.defaultItem
            }
            // we need mock framework extension to mock statics, no user provided => choose default
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

        super.doOKAction()
    }

    /**
     * Creates test source root if absent and target packages for tests.
     */
    private fun createTestRootAndPackages(): Boolean {
        model.testSourceRoot = createDirectoryIfMissing(model.testSourceRoot)
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
            "Generation error"
        )

    private fun findReadOnlyContentEntry(testSourceRoot: VirtualFile?): ContentEntry? {
        if (testSourceRoot == null) return null
        if (testSourceRoot is FakeVirtualFile) {
            return findReadOnlyContentEntry(testSourceRoot.parent)
        }
        return ModuleRootManager.getInstance(model.testModule).contentEntries
            .filterNot { it.file == null }
            .firstOrNull { VfsUtil.isAncestor(it.file!!, testSourceRoot, false) }
    }

    private fun getOrCreateTestRoot(testSourceRoot: VirtualFile): Boolean {
        val modifiableModel = ModuleRootManager.getInstance(model.testModule).modifiableModel
        try {
            val contentEntry = modifiableModel.contentEntries
                .filterNot { it.file == null }
                .firstOrNull { VfsUtil.isAncestor(it.file!!, testSourceRoot, true) }
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
        mockStrategies.item = settings.mockStrategy
        staticsMocking.item = settings.staticsMocking
        parametrizedTestSources.item = settings.parametrizedTestSource

        val areMocksSupported = settings.parametrizedTestSource == ParametrizedTestSource.DO_NOT_PARAMETRIZE
        mockStrategies.isEnabled = areMocksSupported
        staticsMocking.isEnabled = areMocksSupported && mockStrategies.item != MockStrategyApi.NO_MOCKS

        codegenLanguages.item =
            if (model.srcClasses.all { it is KtUltraLightClass }) CodegenLanguage.KOTLIN else CodegenLanguage.JAVA


        val installedTestFramework = TestFramework.allItems.singleOrNull { it.isInstalled }
        currentFrameworkItem = when (parametrizedTestSources.item) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> installedTestFramework ?: settings.testFramework
            ParametrizedTestSource.PARAMETRIZE -> installedTestFramework
                ?: if (settings.testFramework != Junit4) settings.testFramework else TestFramework.parametrizedDefaultItem
        }

        updateTestFrameworksList(settings.parametrizedTestSource)
        updateParametrizationEnabled(currentFrameworkItem)

        updateMockStrategyList()
        updateStaticMockingStrategyList()

        itemsToHelpTooltip.forEach { (box, tooltip) -> tooltip.toolTipText = box.item.description }
    }

    /**
     * This region configures frameworks if required.
     *
     * We need to notify the user about potential problems and to give
     * him a chance to install missing frameworks into his application.
     */
    //region configure frameworks

    private fun configureTestFrameworkIfRequired() {
        val frameworkNotInstalled = !testFrameworks.item.isInstalled

        if (frameworkNotInstalled && createTestFrameworkNotificationDialog() == Messages.YES) {
            configureTestFramework()
        }

        model.hasTestFrameworkConflict = TestFramework.allItems.count { it.isInstalled  } > 1
    }

    private fun configureMockFrameworkIfRequired() {
        val frameworkNotInstalled =
            mockStrategies.item != MockStrategyApi.NO_MOCKS && !MOCKITO.isInstalled

        if (frameworkNotInstalled && createMockFrameworkNotificationDialog() == Messages.YES) {
            configureMockFramework()
        }
    }

    private fun configureStaticMockingIfRequired() {
        val frameworkNotConfigured =
            staticsMocking.item != NoStaticMocking && !staticsMocking.item.isConfigured

        if (frameworkNotConfigured && createStaticsMockingNotificationDialog() == Messages.YES) {
            configureStaticMocking()
        }
    }

    private fun configureTestFramework() {
        val selectedTestFramework = testFrameworks.item

        val libraryInProject =
            findFrameworkLibrary(model.project, model.testModule, selectedTestFramework, LibrarySearchScope.Project)
        val versionInProject = libraryInProject?.libraryName?.parseVersion()

        val libraryDescriptor = when (selectedTestFramework) {
            Junit4 -> jUnit4LibraryDescriptor(versionInProject)
            Junit5 -> jUnit5LibraryDescriptor(versionInProject)
            TestNg -> testNgLibraryDescriptor(versionInProject)
        }

        selectedTestFramework.isInstalled = true
        addDependency(model.testModule, libraryDescriptor)
            .onError { selectedTestFramework.isInstalled = false }
    }

    private fun createTestFrameworkNotificationDialog() = Messages.showYesNoDialog(
        """Selected test framework ${testFrameworks.item.displayName} is not installed into current module. 
            |Would you like to install it now?""".trimMargin(),
        title,
        "Yes",
        "No",
        Messages.getQuestionIcon(),
    )

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

        val staticsMockingValue = staticsMocking.item
        staticsMockingValue.isConfigured = true
    }

    /**
     * Configures Mockito-core to use an experimental feature for static mocking.
     * Returns true if mockito resource have already been configured before.
     *
     * See https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#39
     * for further details.
     */
    private fun configureMockitoResources(testResourcesPath: Path) {
        val mockitoExtensionsPath = "$testResourcesPath/$MOCKITO_EXTENSIONS_STORAGE".toPath()
        val mockitoMockMakerPath = "$mockitoExtensionsPath/$MOCKITO_MOCKMAKER_FILE_NAME".toPath()

        if (!testResourcesPath.exists()) Files.createDirectory(testResourcesPath)
        if (!mockitoExtensionsPath.exists()) Files.createDirectory(mockitoExtensionsPath)

        if (!mockitoMockMakerPath.exists()) {
            Files.createFile(mockitoMockMakerPath)
            Files.write(mockitoMockMakerPath, MOCKITO_EXTENSIONS_FILE_CONTENT)
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
                .lastOrNull { library -> library.libraryName == libraryDescriptor.presentableName }?.let {
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

    private fun createMockFrameworkNotificationDialog() = Messages.showYesNoDialog(
        """Mock framework ${MOCKITO.displayName} is not installed into current module. 
            |Would you like to install it now?""".trimMargin(),
        title,
        "Yes",
        "No",
        Messages.getQuestionIcon(),
    )

    private fun createStaticsMockingNotificationDialog() = Messages.showYesNoDialog(
        """A framework ${MOCKITO.displayName} is not configured to mock static methods.
            |Would you like to configure it now?""".trimMargin(),
        title,
        "Yes",
        "No",
        Messages.getQuestionIcon(),
    )

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
        val codegenLanguage = codegenLanguages.item
        val parametrization = parametrizedTestSources.item

        if (codegenLanguage == CodegenLanguage.KOTLIN
            && parametrization == ParametrizedTestSource.PARAMETRIZE
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

    //language features we use to generate parametrized tests
    // (i.e. @JvmStatic attribute or JUnit5 arguments) are supported since JVM target 1.8
    private val actualKotlinJvmTarget = "1.8"

    private fun setListeners() {
        itemsToHelpTooltip.forEach { (box, tooltip) -> box.setHelpTooltipTextChanger(tooltip) }

        testSourceFolderField.childComponent.addActionListener { event ->
            with((event.source as JComboBox<*>).selectedItem) {
                if (this is VirtualFile) {
                    model.testSourceRoot = this@with
                }
                else {
                    model.testSourceRoot = null
                }
            }
        }

        mockStrategies.addActionListener { event ->
            val comboBox = event.source as ComboBox<*>
            val item = comboBox.item as MockStrategyApi

            staticsMocking.isEnabled = item != MockStrategyApi.NO_MOCKS
            if (!staticsMocking.isEnabled) {
                staticsMocking.item = NoStaticMocking
            }
        }

        testFrameworks.addActionListener { event ->
            val comboBox = event.source as ComboBox<*>
            val item = comboBox.item as TestFramework

            currentFrameworkItem = item
            updateParametrizationEnabled(currentFrameworkItem)
        }

        parametrizedTestSources.addActionListener { event ->
            val comboBox = event.source as ComboBox<*>
            val parametrizedTestSource = comboBox.item as ParametrizedTestSource

            val areMocksSupported = parametrizedTestSource == ParametrizedTestSource.DO_NOT_PARAMETRIZE

            mockStrategies.isEnabled = areMocksSupported
            staticsMocking.isEnabled = areMocksSupported && mockStrategies.item != MockStrategyApi.NO_MOCKS
            if (!mockStrategies.isEnabled) {
                mockStrategies.item = MockStrategyApi.NO_MOCKS
            }
            if (!staticsMocking.isEnabled) {
                staticsMocking.item = NoStaticMocking
            }

            updateTestFrameworksList(parametrizedTestSource)
        }

        cbSpecifyTestPackage.addActionListener {
            val testPackageName = findTestPackageComboValue()
            val packageNameIsNeeded = testPackageField.isEnabled || testPackageName != SAME_PACKAGE_LABEL

            testPackageField.text = if (packageNameIsNeeded) testPackageName else ""
            testPackageField.isEnabled = !testPackageField.isEnabled
        }
    }

    private lateinit var currentFrameworkItem: TestFramework

    //We would like to remove JUnit4 from framework list in parametrized mode
    private fun updateTestFrameworksList(parametrizedTestSource: ParametrizedTestSource) {
        //We do not support parameterized tests for JUnit4
        var enabledTestFrameworks = when (parametrizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> TestFramework.allItems
            ParametrizedTestSource.PARAMETRIZE -> TestFramework.allItems.filterNot { it == Junit4 }
        }

        //Will be removed after gradle-intelij-plugin version update upper than 2020.2
        //TestNg will be reverted after https://github.com/UnitTestBot/UTBotJava/issues/309
        if (findSdkVersion()?.let { it.feature < 11 } == true) {
            enabledTestFrameworks = enabledTestFrameworks.filterNot { it == TestNg }
        }

        var defaultItem = when (parametrizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> TestFramework.defaultItem
            ParametrizedTestSource.PARAMETRIZE -> TestFramework.parametrizedDefaultItem
        }
        enabledTestFrameworks.forEach { if (it.isInstalled && !defaultItem.isInstalled) defaultItem = it }

        testFrameworks.model = DefaultComboBoxModel(enabledTestFrameworks.toTypedArray())
        testFrameworks.item = if (currentFrameworkItem in enabledTestFrameworks) currentFrameworkItem else defaultItem
        testFrameworks.renderer = object : ColoredListCellRenderer<TestFramework>() {
            override fun customizeCellRenderer(
                list: JList<out TestFramework>, value: TestFramework?,
                index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                this.append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                if (value == null || !value.isInstalled) {
                    this.append(WILL_BE_INSTALLED_LABEL, SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
            }
        }

        currentFrameworkItem = testFrameworks.item
    }

    //We would like to disable parametrization options for JUnit4
    private fun updateParametrizationEnabled(testFramework: TestFramework) {
        when (testFramework) {
            Junit4 -> parametrizedTestSources.isEnabled = false
            Junit5,
            TestNg -> parametrizedTestSources.isEnabled = true
        }
    }

    private fun updateMockStrategyList() {
        mockStrategies.renderer = object : ColoredListCellRenderer<MockStrategyApi>() {
            override fun customizeCellRenderer(
                list: JList<out MockStrategyApi>, value: MockStrategyApi?,
                index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                this.append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                if (value != MockStrategyApi.NO_MOCKS && !MOCKITO.isInstalled) {
                    this.append(WILL_BE_INSTALLED_LABEL, SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
            }
        }
    }

    private fun updateStaticMockingStrategyList() {
        staticsMocking.renderer = object : ColoredListCellRenderer<StaticsMocking>() {
            override fun customizeCellRenderer(
                list: JList<out StaticsMocking>, value: StaticsMocking?,
                index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                this.append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                if (value != NoStaticMocking && value?.isConfigured != true) {
                    this.append(WILL_BE_CONFIGURED_LABEL, SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
            }
        }
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

        return entriesPaths.all { path ->
            if (Files.exists(path)) {
                val fileNames = Files.walk(path).map { it.fileName }.toList()
                fileNames.any { it.toString() == MOCKITO_MOCKMAKER_FILE_NAME }
            } else {
                false
            }
        }
    }
}

private fun ComboBox<CodeGenerationSettingItem>.setHelpTooltipTextChanger(helpLabel: JBLabel) {
    addActionListener { event ->
        val comboBox = event.source as ComboBox<*>
        val item = comboBox.item as CodeGenerationSettingItem
        helpLabel.toolTipText = item.description
    }
}