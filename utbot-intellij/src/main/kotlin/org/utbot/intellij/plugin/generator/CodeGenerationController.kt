package org.utbot.intellij.plugin.generator

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.fileTemplates.JavaTemplateUtil
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.util.IncorrectOperationException
import com.siyeh.ig.psiutils.ImportUtils
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.util.ImportInsertHelperImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.scripting.resolve.classId
import org.utbot.common.HTML_LINE_SEPARATOR
import org.utbot.common.PathUtil.toHtmlLinkTag
import org.utbot.common.allNestedClasses
import org.utbot.common.appendHtmlLine
import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.CodeGenerationResult
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.UtilClassKind.Companion.PACKAGE_DELIMITER
import org.utbot.framework.codegen.model.UtilClassKind.Companion.UT_UTILS_CLASS_NAME
import org.utbot.framework.codegen.model.UtilClassKind.Companion.UT_UTILS_PACKAGE_NAME
import org.utbot.framework.codegen.model.UtilClassKind.UtUtilsWithMockito
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.util.Conflict
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.sarif.SarifReportIdea
import org.utbot.intellij.plugin.sarif.SourceFindingStrategyIdea
import org.utbot.intellij.plugin.ui.DetailsTestsReportNotifier
import org.utbot.intellij.plugin.ui.SarifReportNotifier
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.ui.TestsReportNotifier
import org.utbot.intellij.plugin.ui.WarningTestsReportNotifier
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.util.RunConfigurationHelper
import org.utbot.intellij.plugin.util.extractClassMethodsIncludingNested
import org.utbot.intellij.plugin.util.signature
import org.utbot.sarif.SarifReport
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import org.utbot.intellij.plugin.util.IntelliJApiHelper.Target.*
import org.utbot.intellij.plugin.util.IntelliJApiHelper.run

object CodeGenerationController {

    private class UtilMethodListener {
        var requiredUtilClassKind: UtilClassKind? = null

        fun onTestClassGenerated(result: CodeGenerationResult) {
            requiredUtilClassKind = maxOfNullable(requiredUtilClassKind, result.utilClassKind)
        }

        private fun <T : Comparable<T>> maxOfNullable(a: T?, b: T?): T? {
            return when {
                a == null -> b
                b == null -> a
                else -> maxOf(a, b)
            }
        }
    }

    fun generateTests(
        model: GenerateTestsModel,
        testSetsByClass: Map<PsiClass, List<UtMethodTestSet>>,
        psi2KClass: Map<PsiClass, KClass<*>>
    ) {
        val baseTestDirectory = model.testSourceRoot?.toPsiDirectory(model.project)
            ?: return
        val allTestPackages = getPackageDirectories(baseTestDirectory)
        val latch = CountDownLatch(testSetsByClass.size)

        val reports = mutableListOf<TestsGenerationReport>()
        val testFiles = mutableListOf<PsiFile>()
        val utilMethodListener = UtilMethodListener()
        for (srcClass in testSetsByClass.keys) {
            val testSets = testSetsByClass[srcClass] ?: continue
            try {
                val classPackageName = model.getTestClassPackageNameFor(srcClass)
                val testDirectory = allTestPackages[classPackageName] ?: baseTestDirectory
                val testClass = createTestClass(srcClass, testDirectory, model) ?: continue
                val testClassFile = testClass.containingFile
                val cut = psi2KClass[srcClass] ?: error("Didn't find KClass instance for class ${srcClass.name}")
                runWriteCommandAction(model.project, "Generate tests with UtBot", null, {
                    try {
                        generateCodeAndReport(
                            srcClass,
                            cut,
                            testClass,
                            testClassFile,
                            testSets,
                            model,
                            latch,
                            reports,
                            utilMethodListener
                        )
                        testFiles.add(testClassFile)
                    } catch (e: IncorrectOperationException) {
                        showCreatingClassError(model.project, createTestClassName(srcClass))
                    }
                })
            } catch (e: IncorrectOperationException) {
                showCreatingClassError(model.project, createTestClassName(srcClass))
            }
        }


        run(EDT_LATER) {
            waitForCountDown(latch, timeout = 100, timeUnit = TimeUnit.MILLISECONDS) {
                val utilClassKind = utilMethodListener.requiredUtilClassKind
                if (utilClassKind != null) {
                    // create a directory to put utils class into
                    val utilClassDirectory = createUtUtilSubdirectories(baseTestDirectory)

                    val language = model.codegenLanguage
                    val utUtilsName = "$UT_UTILS_CLASS_NAME${language.extension}"

                    val utUtilsAlreadyExists = utilClassDirectory.findFile(utUtilsName) != null

                    // we generate and write an util class in one of the two cases:
                    // - util file does not yet exist --> then we generate it, because it is required by generated tests
                    // - utilClassKind is UtUtilsWithMockito --> then we generate utils class and add it to utils directory.
                    //   If utils file already exists, we overwrite it, because existing utils may be without Mockito,
                    //   and we want to make sure that the generated utils use Mockito.
                    if (!utUtilsAlreadyExists || utilClassKind is UtUtilsWithMockito) {
                        generateAndWriteUtilClass(utUtilsName, utilClassDirectory, model, utilClassKind)
                    }
                }
            }
        }

        run(READ_ACTION) {
            val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
            run(THREAD_POOL) {
                waitForCountDown(latch) {
                    try {
                        // Parametrized tests are not supported in tests report yet
                        // TODO JIRA:1507
                        if (model.parametrizedTestSource != ParametrizedTestSource.PARAMETRIZE) {
                            showTestsReport(reports, model)
                        }
                    } catch (e: Exception) {
                        showErrorDialogLater(
                            model.project,
                            message = "Cannot save tests generation report: error occurred '${e.message}'",
                            title = "Failed to save tests report"
                        )
                    }

                    mergeSarifReports(model, sarifReportsPath)
                    if (model.runGeneratedTestsWithCoverage) {
                        RunConfigurationHelper.runTestsWithCoverage(model, testFiles)
                    }
                }
            }
        }
    }

    /**
     * Create package directories if needed for UtUtils class.
     * Then generate and create a UtUtils class file in the utils package.
     * Also run reformatting for the generated class.
     */
    private fun generateAndWriteUtilClass(
        utUtilsName: String,
        utilClassDirectory: PsiDirectory,
        model: GenerateTestsModel,
        utilClassKind: UtilClassKind
    ) {
        val psiDocumentManager = PsiDocumentManager.getInstance(model.project)

        val utUtilsText = utilClassKind.getUtilClassText(model.codegenLanguage)

        val existingUtUtilsDocument = utilClassDirectory
            .findFile(utUtilsName)
            ?.let { psiDocumentManager.getDocument(it) }

        val utUtilsFile = if (existingUtUtilsDocument != null) {
            executeCommand {
                existingUtUtilsDocument.setText(utUtilsText)
            }
            unblockDocument(model.project, existingUtUtilsDocument)
            psiDocumentManager.getPsiFile(existingUtUtilsDocument)
        } else {
            val utUtilsFile = PsiFileFactory.getInstance(model.project)
                .createFileFromText(
                    utUtilsName,
                    model.codegenLanguage.fileType,
                    utUtilsText
                )
            // add the UtUtils class file into the utils directory
            runWriteCommandAction(model.project) {
                utilClassDirectory.add(utUtilsFile)
            }
            utUtilsFile
        }

        // there's only one class in the file
        val utUtilsClass = (utUtilsFile as PsiClassOwner).classes.first()

        runWriteCommandAction(model.project, "UtBot util class reformatting", null, {
            reformat(model, utUtilsFile, utUtilsClass)
        })

        val utUtilsDocument = psiDocumentManager.getDocument(utUtilsFile)
            ?: error("Failed to get a Document for UtUtils file")

        unblockDocument(model.project, utUtilsDocument)
    }

    /**
     * @param srcClass class under test
     * @return name of the package of a given [srcClass].
     * Null is returned if [PsiDirectory.getPackage] call returns null for the [srcClass] directory.
     */
    private fun GenerateTestsModel.getTestClassPackageNameFor(srcClass: PsiClass): String? {
        return when {
            testPackageName.isNullOrEmpty() -> srcClass.containingFile.containingDirectory.getPackage()?.qualifiedName
            else -> testPackageName
        }
    }

    /**
     * Create all package directories for UtUtils class.
     * @return the innermost directory - utils from `org.utbot.runtime.utils`
     */
    private fun createUtUtilSubdirectories(baseTestDirectory: PsiDirectory): PsiDirectory {
        val directoryNames = UT_UTILS_PACKAGE_NAME.split(PACKAGE_DELIMITER)
        var currentDirectory = baseTestDirectory
        runWriteCommandAction(baseTestDirectory.project) {
            for (name in directoryNames) {
                currentDirectory = currentDirectory.findSubdirectory(name) ?: currentDirectory.createSubdirectory(name)
            }
        }
        return currentDirectory
    }

    /**
     * @return Java or Kotlin file type depending on the given [CodegenLanguage]
     */
    private val CodegenLanguage.fileType: FileType
        get() = when (this) {
            CodegenLanguage.JAVA -> JavaFileType.INSTANCE
            CodegenLanguage.KOTLIN -> KotlinFileType.INSTANCE
        }

    private fun waitForCountDown(latch: CountDownLatch, timeout: Long = 5, timeUnit: TimeUnit = TimeUnit.SECONDS, action: Runnable) {
        try {
            if (!latch.await(timeout, timeUnit)) {
                run(THREAD_POOL) { waitForCountDown(latch, timeout, timeUnit, action) }
            } else {
                action.run()
            }
        } catch (ignored: InterruptedException) {
        }
    }

    private fun mergeSarifReports(model: GenerateTestsModel, sarifReportsPath: Path) {
        val mergedReportFile = sarifReportsPath
            .resolve("${model.project.name}Report.sarif")
            .toFile()
        // deleting the old report so that `sarifReports` does not contain it
        mergedReportFile.delete()

        val sarifReports = sarifReportsPath.toFile()
            .walkTopDown()
            .filter { it.extension == "sarif" }
            .map { it.readText() }
            .toList()

        val mergedReport = SarifReport.mergeReports(sarifReports)
        mergedReportFile.writeText(mergedReport)

        // notifying the user
        SarifReportNotifier.notify(
            info = """
                SARIF report was saved to ${toHtmlLinkTag(mergedReportFile.path)}$HTML_LINE_SEPARATOR
            """.trimIndent()
        )
    }

    private fun getPackageDirectories(baseDirectory: PsiDirectory): Map<String, PsiDirectory> {
        val allSubdirectories = mutableMapOf<String, PsiDirectory>()
        getPackageDirectoriesRecursively(baseDirectory, allSubdirectories)

        return allSubdirectories
    }

    private fun getPackageDirectoriesRecursively(
        baseDirectory: PsiDirectory,
        innerPackageNames: MutableMap<String, PsiDirectory>,
    ) {
        baseDirectory.getPackage()?.qualifiedName?.let { innerPackageNames[it] = baseDirectory }
        for (subDir in baseDirectory.subdirectories) {
            getPackageDirectoriesRecursively(subDir, innerPackageNames)
        }
    }

    private fun createTestClass(srcClass: PsiClass, testDirectory: PsiDirectory, model: GenerateTestsModel): PsiClass? {
        val testClassName = createTestClassName(srcClass)
        val aPackage = JavaDirectoryService.getInstance().getPackage(testDirectory)

        if (aPackage != null) {
            val scope = GlobalSearchScopesCore.directoryScope(testDirectory, false)

            val application = ApplicationManager.getApplication()
            val testClass = application.executeOnPooledThread<PsiClass?> {
                return@executeOnPooledThread application.runReadAction<PsiClass?> {
                    DumbService.getInstance(model.project).runReadActionInSmartMode(Computable<PsiClass?> {
                        // Here we use firstOrNull(), because by some unknown reason
                        // findClassByShortName() may return two identical objects.
                        // Be careful, do not use singleOrNull() here, because it expects
                        // the array to contain strictly one element and otherwise returns null.
                        return@Computable aPackage.findClassByShortName(testClassName, scope)
                            .firstOrNull {
                                when (model.codegenLanguage) {
                                    CodegenLanguage.JAVA -> it !is KtUltraLightClass
                                    CodegenLanguage.KOTLIN -> it is KtUltraLightClass
                                }
                            }
                    })
                }
            }.get()

            testClass?.let {
                return if (FileModificationService.getInstance().preparePsiElementForWrite(it)) it else null
            }
        }

        val fileTemplate = FileTemplateManager.getInstance(testDirectory.project).getInternalTemplate(
            when (model.codegenLanguage) {
                CodegenLanguage.JAVA -> JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME
                CodegenLanguage.KOTLIN -> "Kotlin Class"
            }
        )
        runWriteAction { testDirectory.findFile(testClassName + model.codegenLanguage.extension)?.delete() }
        val createFromTemplate: PsiElement = FileTemplateUtil.createFromTemplate(
            fileTemplate,
            testClassName,
            FileTemplateManager.getInstance(testDirectory.project).defaultProperties,
            testDirectory
        )

        return (createFromTemplate.containingFile as PsiClassOwner).classes.first()
    }

    private fun generateCodeAndReport(
        srcClass: PsiClass,
        classUnderTest: KClass<*>,
        testClass: PsiClass,
        file: PsiFile,
        testSets: List<UtMethodTestSet>,
        model: GenerateTestsModel,
        reportsCountDown: CountDownLatch,
        reports: MutableList<TestsGenerationReport>,
        utilMethodListener: UtilMethodListener
    ) {
        val classMethods = srcClass.extractClassMethodsIncludingNested(false)
        val paramNames = DumbService.getInstance(model.project)
            .runReadActionInSmartMode(Computable { findMethodParamNames(classUnderTest, classMethods) })

        val codeGenerator = CodeGenerator(
            classUnderTest = classUnderTest.id,
            generateUtilClassFile = true,
            paramNames = paramNames.toMutableMap(),
            testFramework = model.testFramework,
            mockFramework = model.mockFramework,
            codegenLanguage = model.codegenLanguage,
            parameterizedTestSource = model.parametrizedTestSource,
            staticsMocking = model.staticsMocking,
            forceStaticMocking = model.forceStaticMocking,
            generateWarningsForStaticMocking = model.generateWarningsForStaticMocking,
            runtimeExceptionTestsBehaviour = model.runtimeExceptionTestsBehaviour,
            hangingTestsTimeout = model.hangingTestsTimeout,
            enableTestsTimeout = true,
            testClassPackageName = testClass.packageName
        )

        val editor = CodeInsightUtil.positionCursorAtLBrace(testClass.project, file, testClass)
        //TODO: Use PsiDocumentManager.getInstance(model.project).getDocument(file)
        // if we don't want to open _all_ new files with tests in editor one-by-one
        run(THREAD_POOL) {
            val codeGenerationResult = codeGenerator.generateAsStringWithTestReport(testSets)
            utilMethodListener.onTestClassGenerated(codeGenerationResult)
            val generatedTestsCode = codeGenerationResult.generatedCode
            run(EDT_LATER) {
                run(WRITE_ACTION) {
                    unblockDocument(testClass.project, editor.document)
                    // TODO: JIRA:1246 - display warnings if we rewrite the file
                    executeCommand(testClass.project, "Insert Generated Tests") {
                        editor.document.setText(generatedTestsCode)
                    }
                    unblockDocument(testClass.project, editor.document)

                    // after committing the document the `testClass` is invalid in PsiTree,
                    // so we have to reload it from the corresponding `file`
                    val testClassUpdated = (file as PsiClassOwner).classes.first() // only one class in the file

                    // reformatting before creating reports due to
                    // SarifReport requires the final version of the generated tests code
                    run(THREAD_POOL) {
                        IntentionHelper(model.project, editor, file).applyIntentions()
                        run(EDT_LATER) {
                            runWriteCommandAction(testClassUpdated.project, "UtBot tests reformatting", null, {
                                reformat(model, file, testClassUpdated)
                            })
                            unblockDocument(testClassUpdated.project, editor.document)

                            // uploading formatted code
                            val codeGenerationResultFormatted =
                                codeGenerationResult.copy(generatedCode = file.text)

                            // creating and saving reports
                            reports += codeGenerationResultFormatted.testsGenerationReport

                            run(WRITE_ACTION) {
                                saveSarifReport(
                                    testClassUpdated,
                                    testSets,
                                    model,
                                    codeGenerationResultFormatted,
                                )
                            }
                            unblockDocument(testClassUpdated.project, editor.document)

                            reportsCountDown.countDown()
                        }
                    }
                }
            }
        }
    }

    private fun reformat(model: GenerateTestsModel, file: PsiFile, testClass: PsiClass) {
        val project = model.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        codeStyleManager.reformat(file)
        when (model.codegenLanguage) {
            CodegenLanguage.JAVA -> {
                val range = file.textRange
                val startOffset = range.startOffset
                val endOffset = range.endOffset
                val reformatRange = codeStyleManager.reformatRange(file, startOffset, endOffset, false)
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(reformatRange)
            }
            CodegenLanguage.KOTLIN -> ShortenReferences.DEFAULT.process((testClass as KtUltraLightClass).kotlinOrigin.containingKtFile)
        }
    }

    private fun findMethodParamNames(clazz: KClass<*>, methods: List<MemberInfo>): Map<ExecutableId, List<String>> {
        val bySignature = methods.associate { it.signature() to it.paramNames() }
        return clazz.allNestedClasses.flatMap { it.functions }
            .mapNotNull { method -> bySignature[method.signature()]?.let { params -> method.executableId to params } }
            .toMap()
    }

    private fun MemberInfo.paramNames(): List<String> =
        (this.member as PsiMethod).parameterList.parameters.map { it.name }

    private fun saveSarifReport(
        testClass: PsiClass,
        testSets: List<UtMethodTestSet>,
        model: GenerateTestsModel,
        testsCodeWithTestReport: CodeGenerationResult,
    ) {
        val project = model.project
        val generatedTestsCode = testsCodeWithTestReport.generatedCode

        try {
            // saving sarif report
            val sourceFinding = SourceFindingStrategyIdea(testClass)
            executeCommand(testClass.project, "Saving Sarif report") {
                SarifReportIdea.createAndSave(model, testSets, generatedTestsCode, sourceFinding)
            }
        } catch (e: Exception) {
            showErrorDialogLater(
                project,
                message = "Cannot save Sarif report via generated tests: error occurred '${e.message}'",
                title = "Failed to save Sarif report"
            )
        }
    }

    private fun isEventLogAvailable(project: Project) =
        ToolWindowManager.getInstance(project).getToolWindow("Event Log") != null

    private fun eventLogMessage(): String =
        """
            <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.eventLogSuffix}">See details in Event Log</a>.
        """.trimIndent()

    private fun destinationWarningMessage(testPackageName: String?, classUnderTestPackageName: String): String? {
        return if (classUnderTestPackageName != testPackageName) {
            """
            Warning: Destination package $testPackageName does not match package of the class $classUnderTestPackageName.
            This may cause unnecessary usage of reflection for protected or package-private fields and methods access.
        """.trimIndent()
        } else {
            null
        }
    }

    private fun showTestsReport(reports: List<TestsGenerationReport>, model: GenerateTestsModel) {
        var hasWarnings = false
        require(reports.isNotEmpty())

        val (notifyMessage, statistics) = if (reports.size == 1) {
            val report = reports.first()
            processInitialWarnings(report, model)

            val message = buildString {
                appendHtmlLine(report.toString(isShort = true))

                val classUnderTestPackageName =
                    report.classUnderTest.classId.packageFqName.toString()

                    destinationWarningMessage(model.testPackageName, classUnderTestPackageName)
                        ?.let {
                            hasWarnings = true
                            appendHtmlLine(it)
                            appendHtmlLine()
                        }
                if (isEventLogAvailable(model.project)) {
                    appendHtmlLine(eventLogMessage())
                }
            }
            hasWarnings = hasWarnings || report.hasWarnings
            Pair(message, report.detailedStatistics)
        } else {
            val accumulatedReport = reports.first()
            processInitialWarnings(accumulatedReport, model)

            val message = buildString {
                appendHtmlLine("${reports.sumBy { it.executables.size }} tests generated for ${reports.size} classes.")

                if (accumulatedReport.initialWarnings.isNotEmpty()) {
                    accumulatedReport.initialWarnings.forEach { appendHtmlLine(it()) }
                    appendHtmlLine()
                }

                // TODO maybe add statistics info here

                for (report in reports) {
                    val classUnderTestPackageName =
                        report.classUnderTest.classId.packageFqName.toString()

                    hasWarnings = hasWarnings || report.hasWarnings
                    if (!model.isMultiPackage) {
                        val destinationWarning =
                            destinationWarningMessage(model.testPackageName, classUnderTestPackageName)
                        if (destinationWarning != null) {
                            hasWarnings = true
                            appendHtmlLine(destinationWarning)
                            appendHtmlLine()
                        }
                    }
                }
                if (isEventLogAvailable(model.project)) {
                    appendHtmlLine(eventLogMessage())
                }
            }

            Pair(message, null)
        }

        if (hasWarnings) {
            WarningTestsReportNotifier.notify(notifyMessage)
        } else {
            TestsReportNotifier.notify(notifyMessage)
        }

        statistics?.let { DetailsTestsReportNotifier.notify(it) }
    }

    private fun processInitialWarnings(report: TestsGenerationReport, model: GenerateTestsModel) {
        val hasInitialWarnings = model.conflictTriggers.triggered

        if (!hasInitialWarnings) {
            return
        }

        report.apply {
            if (model.conflictTriggers[Conflict.ForceMockHappened] == true) {
                initialWarnings.add {
                    """
                    <b>Warning</b>: Some test cases were ignored, because no mocking framework is installed in the project.<br>
                    Better results could be achieved by <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.mockitoSuffix}">installing mocking framework</a>.
                """.trimIndent()
                }
            }
            if (model.conflictTriggers[Conflict.ForceStaticMockHappened] == true) {
                initialWarnings.add {
                    """
                    <b>Warning</b>: Some test cases were ignored, because mockito-inline is not installed in the project.<br>
                    Better results could be achieved by <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.mockitoInlineSuffix}">configuring mockito-inline</a>.
                """.trimIndent()
                }
            }
            if (model.conflictTriggers[Conflict.TestFrameworkConflict] == true) {
                initialWarnings.add {
                    """
                    <b>Warning</b>: There are several test frameworks in the project. 
                    To select run configuration, please refer to the documentation depending on the project build system:
                     <a href=" https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests">Gradle</a>, 
                     <a href=" https://maven.apache.org/surefire/maven-surefire-plugin/examples/providers.html">Maven</a> 
                     or <a href=" https://www.jetbrains.com/help/idea/run-debug-configuration.html#compound-configs">Idea</a>.
                """.trimIndent()
                }
            }
        }
    }

    @Suppress("unused")
    // this method was used in the past, not used in the present but may be used in the future
    private fun insertImports(testClass: PsiClass, imports: List<Import>, editor: Editor) {
        unblockDocument(testClass.project, editor.document)
        executeCommand(testClass.project, "Insert Generated Tests") {
            imports.forEach { import ->
                when (import) {
                    is StaticImport -> {
                        if (testClass is KtUltraLightClass) {
                            ImportInsertHelperImpl.addImport(
                                (testClass as PsiClass).project,
                                testClass.kotlinOrigin.containingKtFile,
                                FqName(import.qualifiedName)
                            )
                        } else {
                            ImportUtils.addStaticImport(import.qualifierClass, import.memberName, testClass)
                        }
                    }
                    is RegularImport -> { }
                }
            }
        }
        unblockDocument(testClass.project, editor.document)
    }

    private fun createTestClassName(srcClass: PsiClass) = srcClass.name + "Test"

    @Suppress("unused")
    // this method was used in the past, not used in the present but may be used in the future
    private fun insertMethods(testClass: PsiClass, superBody: String, editor: Editor) {
        val dummyMethod = TestIntegrationUtils.createDummyMethod(testClass)
        if (testClass is KtUltraLightClass) {
            val ktClass = testClass.kotlinOrigin as KtClass
            val factory = KtPsiFactory((testClass as PsiClass).project)
            val function: KtNamedFunction = factory.createFunction(dummyMethod.text)
            val addDeclaration: KtNamedFunction = ktClass.addDeclaration(function)

            unblockDocument(testClass.project, editor.document)
            executeCommand(testClass.project, "Insert Generated Tests") {
                replace(editor, addDeclaration, superBody)
            }
            unblockDocument(testClass.project, editor.document)
        } else {
            val method = (testClass.add(dummyMethod)) as PsiMethod
            unblockDocument(testClass.project, editor.document)
            executeCommand(testClass.project, "Insert Generated Tests") {
                replace(editor, method, superBody)
            }
            unblockDocument(testClass.project, editor.document)
        }
    }

    private fun unblockDocument(project: Project, document: Document) {
        PsiDocumentManager.getInstance(project).apply {
            commitDocument(document)
            doPostponedOperationsAndUnblockDocument(document)
        }
    }

    private fun replace(editor: Editor, element: PsiElement, body: String) {
        val startOffset = element.startOffset
        val endOffset = element.endOffset
        editor.document.replaceString(startOffset, endOffset, body)
    }

    private fun showCreatingClassError(project: Project, testClassName: String) {
        showErrorDialogLater(
            project,
            message = "Cannot Create Class '$testClassName'",
            title = "Failed to Create Class"
        )
    }
}
