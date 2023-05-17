package org.utbot.intellij.plugin.generator

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.fileTemplates.JavaTemplateUtil
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager.DO_NOT_ADD_IMPORTS
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.testIntegration.TestIntegrationUtils
import com.siyeh.ig.psiutils.ImportUtils
import mu.KotlinLogging
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
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.utbot.common.FileUtil
import org.utbot.common.HTML_LINE_SEPARATOR
import org.utbot.common.PathUtil.toHtmlLinkTag
import org.utbot.framework.CancellationStrategyType.CANCEL_EVERYTHING
import org.utbot.framework.CancellationStrategyType.NONE
import org.utbot.framework.CancellationStrategyType.SAVE_PROCESSED_RESULTS
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.Import
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RegularImport
import org.utbot.framework.codegen.domain.StaticImport
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.codegen.tree.ututils.UtilClassKind.Companion.UT_UTILS_INSTANCE_NAME
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.intellij.plugin.inspection.UnitTestBotInspectionManager
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.intellij.plugin.process.RdTestGenerationResult
import org.utbot.intellij.plugin.sarif.SarifReportIdea
import org.utbot.intellij.plugin.ui.CommonLoggingNotifier
import org.utbot.intellij.plugin.ui.DetailsTestsReportNotifier
import org.utbot.intellij.plugin.ui.SarifReportNotifier
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.ui.TestsReportNotifier
import org.utbot.intellij.plugin.ui.WarningTestsReportNotifier
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots
import org.utbot.intellij.plugin.util.IntelliJApiHelper.Target.EDT_LATER
import org.utbot.intellij.plugin.util.IntelliJApiHelper.Target.THREAD_POOL
import org.utbot.intellij.plugin.util.IntelliJApiHelper.Target.WRITE_ACTION
import org.utbot.intellij.plugin.util.IntelliJApiHelper.run
import org.utbot.intellij.plugin.util.RunConfigurationHelper
import org.utbot.intellij.plugin.util.assertIsDispatchThread
import org.utbot.intellij.plugin.util.assertIsWriteThread
import org.utbot.intellij.plugin.util.extractClassMethodsIncludingNested
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.utbot.intellij.plugin.util.showSettingsEditor
import org.utbot.sarif.*

object CodeGenerationController {
    private val logger = KotlinLogging.logger {}

    private class UtilClassListener {
        var requiredUtilClassKind: UtilClassKind? = null

        fun onTestClassGenerated(result: UtilClassKind?) {
            requiredUtilClassKind = maxOfNullable(requiredUtilClassKind, result)
        }
    }

    fun generateTests(
        model: GenerateTestsModel,
        classesWithTests: Map<PsiClass, RdTestGenerationResult>,
        psi2KClass: Map<PsiClass, ClassId>,
        process: EngineProcess,
        indicator: ProgressIndicator
    ) {
        assertIsDispatchThread()
        val baseTestDirectory = model.testSourceRoot?.toPsiDirectory(model.project)
            ?: return
        val allTestPackages = getPackageDirectories(baseTestDirectory)
        val latch = CountDownLatch(classesWithTests.size)
        val testFilesPointers = mutableListOf<SmartPsiElementPointer<PsiFile>>()
        val srcClassPathToSarifReport = mutableMapOf<Path, Sarif>()
        val utilClassListener = UtilClassListener()
        var index = 0
        for ((srcClass, generateResult) in classesWithTests) {
            if (indicator.isCanceled) {
                when (UtSettings.cancellationStrategyType) {
                    NONE,
                    SAVE_PROCESSED_RESULTS -> {}
                    CANCEL_EVERYTHING -> break
                }
            }

            val (count, testSetsId) = generateResult
            if (count <= 0)  {
                latch.countDown()
                continue
            }
            try {
                UtTestsDialogProcessor.updateIndicator(indicator, UtTestsDialogProcessor.ProgressRange.CODEGEN, "Write test cases for class ${srcClass.name}", index.toDouble() / classesWithTests.size)
                val classPackageName = model.getTestClassPackageNameFor(srcClass)
                val testDirectory = allTestPackages[classPackageName] ?: baseTestDirectory
                val testClass = createTestClass(srcClass, testDirectory, model) ?: continue
                val testFilePointer = SmartPointerManager.getInstance(model.project)
                    .createSmartPsiElementPointer(testClass.containingFile)
                val cut = psi2KClass[srcClass] ?: error("Didn't find KClass instance for class ${srcClass.name}")
                runWriteCommandAction(model.project, "Generate tests with UnitTestBot", null, {
                    generateCodeAndReport(
                        process,
                        testSetsId,
                        srcClass,
                        cut,
                        testClass,
                        testFilePointer,
                        srcClassPathToSarifReport,
                        model,
                        latch,
                        utilClassListener,
                        indicator
                    )
                    testFilesPointers.add(testFilePointer)
                })
            } catch (e : CancellationException) {
                throw e
            } catch (e: Exception) {
                showCreatingClassError(model.project, createTestClassName(srcClass))
            } finally {
                index++
            }
        }

        run(THREAD_POOL, indicator, "Waiting for per-class Sarif reports") {
            waitForCountDown(latch, indicator = indicator) {
                run(EDT_LATER, indicator,"Go to EDT for utility class creation") {
                    run(WRITE_ACTION, indicator, "Need write action for utility class creation") {
                        createUtilityClassIfNeeded(utilClassListener, model, baseTestDirectory, indicator)
                        run(THREAD_POOL, indicator, "Generate summary Sarif report") {
                            proceedTestReport(process, model)
                            val sarifReportsPath =
                                model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
                            UtTestsDialogProcessor.updateIndicator(indicator, UtTestsDialogProcessor.ProgressRange.SARIF, "Merge Sarif reports", 0.75)
                            mergeSarifReports(model, sarifReportsPath)
                            if (model.runGeneratedTestsWithCoverage) {
                                UtTestsDialogProcessor.updateIndicator(indicator, UtTestsDialogProcessor.ProgressRange.SARIF, "Start tests with coverage", 0.95)
                                RunConfigurationHelper.runTestsWithCoverage(model, testFilesPointers)
                            }
                            process.terminate()
                            UtTestsDialogProcessor.updateIndicator(indicator, UtTestsDialogProcessor.ProgressRange.SARIF, "Generation finished", 1.0)

                            run(EDT_LATER, null, "Run sarif-based inspections") {
                                runInspectionsIfNeeded(model, srcClassPathToSarifReport)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Runs the UTBot inspection if there are detected errors.
     */
    private fun runInspectionsIfNeeded(
        model: GenerateTestsModel,
        srcClassPathToSarifReport: MutableMap<Path, Sarif>
    ) {
        if (!model.runInspectionAfterTestGeneration) {
            return
        }
        val sarifHasResults = srcClassPathToSarifReport.any { (_, sarif) ->
            sarif.getAllResults().isNotEmpty()
        }
        if (!sarifHasResults) {
            return
        }
        UnitTestBotInspectionManager
            .getInstance(model.project, SarifReport.minimizeSarifResults(srcClassPathToSarifReport))
            .createNewGlobalContext()
            .doInspections(AnalysisScope(model.project))
    }

    private fun proceedTestReport(proc: EngineProcess, model: GenerateTestsModel) {
        try {
            // Parametrized tests are not supported in tests report yet
            // TODO JIRA:1507
            if (model.parametrizedTestSource != ParametrizedTestSource.PARAMETRIZE) {
                showTestsReport(proc, model)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save tests report" }
            showErrorDialogLater(
                model.project,
                message = "Cannot save tests generation report: error occurred '${e.message}'",
                title = "Failed to save tests report"
            )
        }
    }

    private fun createUtilityClassIfNeeded(
        utilClassListener: UtilClassListener,
        model: GenerateTestsModel,
        baseTestDirectory: PsiDirectory,
        indicator: ProgressIndicator
    ) {
        val requiredUtilClassKind = utilClassListener.requiredUtilClassKind
            ?: return // no util class needed

        val existingUtilClass = model.codegenLanguage.getUtilClassOrNull(model.project, model.testModule)
        val utilClassKind = newUtilClassKindOrNull(existingUtilClass, requiredUtilClassKind, model.codegenLanguage)
        if (utilClassKind != null) {
            createOrUpdateUtilClass(
                testDirectory = baseTestDirectory,
                utilClassKind = utilClassKind,
                existingUtilClass = existingUtilClass,
                model = model,
                indicator = indicator
            )
        }
    }

    /**
     * This method decides whether to overwrite an existing util class with a new one. And if so, then with what kind of util class.
     * - If no util class exists, then we generate a new one.
     * - If existing util class' version is out of date, then we overwrite it with a new one.
     * But we use the maximum of two kinds (existing and the new one) to avoid problems with mocks.
     * - If existing util class is up-to-date **and** has a greater or equal priority than the new one,
     * then we do not need to overwrite it (return null).
     * - Lastly, if the new util class kind has a greater priority than the existing one,
     * then we do overwrite it with a newer version.
     *
     * @param existingUtilClass a [PsiFile] representing a file of an existing util class. If it does not exist, then [existingUtilClass] is `null`.
     * @param requiredUtilClassKind the kind of the new util class that we attempt to generate.
     * @return an [UtilClassKind] of a new util class that will be created or `null`, if no new util class is needed.
     */
    private fun newUtilClassKindOrNull(
        existingUtilClass: PsiFile?,
        requiredUtilClassKind: UtilClassKind,
        codegenLanguage: CodegenLanguage,
    ): UtilClassKind? {
        if (existingUtilClass == null) {
            // If no util class exists, then we should create a new one with the given kind.
            return requiredUtilClassKind
        }

        val existingUtilClassVersion = existingUtilClass.utilClassVersionOrNull ?: return requiredUtilClassKind
        val newUtilClassVersion = requiredUtilClassKind.utilClassVersion(codegenLanguage)
        val versionIsUpdated = existingUtilClassVersion != newUtilClassVersion

        val existingUtilClassKind = existingUtilClass.utilClassKindOrNull(codegenLanguage) ?: return requiredUtilClassKind

        if (versionIsUpdated) {
            // If an existing util class is out of date, then we must overwrite it with a newer version.
            // But we choose the kind with more priority, because it is possible that
            // the existing util class needed mocks, but the new one doesn't.
            // In this case we still want to support mocks, because the previously generated tests
            // expect that the util class does support them.
            return maxOfNullable(existingUtilClassKind, requiredUtilClassKind)
        }

        if (requiredUtilClassKind <= existingUtilClassKind) {
            // If the existing util class kind has a greater or equal priority than the new one we attempt to generate,
            // then we should not do anything. The existing util class is already enough.
            return null
        }

        // The last case. The existing util class has a strictly less priority than the new one.
        // So we generate the new one to overwrite the previous one with it.
        return requiredUtilClassKind
    }

    /**
     * If [existingUtilClass] is null (no util class exists), then we create package directories for util class,
     * create util class itself, and put it into the corresponding directory.
     * Otherwise, we overwrite the existing util class with a new one.
     * This is necessary in case if existing util class has no mocks support, but the newly generated tests do use mocks.
     * So, we overwrite an util class with a new one that does support mocks.
     *
     * @param testDirectory root test directory where we will put our generated tests.
     * @param utilClassKind kind of util class required by the test class(es) that we generated.
     * @param existingUtilClass util class that already exists or null if it does not yet exist.
     * @param model [GenerateTestsModel] that contains some useful information for util class generation.
     */
    private fun createOrUpdateUtilClass(
        testDirectory: PsiDirectory,
        utilClassKind: UtilClassKind,
        existingUtilClass: PsiFile?,
        model: GenerateTestsModel,
        indicator: ProgressIndicator
    ) {
        val language = model.codegenLanguage

        val utUtilsFile = if (existingUtilClass == null) {
            // create a directory to put utils class into
            val utilClassDirectory = createUtUtilSubdirectories(testDirectory, language)
            // create util class file and put it into utils directory
            createNewUtilClass(utilClassDirectory, language, utilClassKind, model)
        } else {
            overwriteUtilClass(existingUtilClass, utilClassKind, model, indicator)
        }

        val utUtilsClass = runReadAction {
            // there's only one class in the file
            (utUtilsFile as PsiClassOwner).classes.first()
        }

        runWriteCommandAction(model.project, "UnitTestBot util class reformatting", null, {
            reformat(model, SmartPointerManager.getInstance(model.project).createSmartPsiElementPointer(utUtilsFile), utUtilsClass)
        })

        val utUtilsDocument = runReadAction {
            FileDocumentManager
                .getInstance()
                .getDocument(utUtilsFile.viewProvider.virtualFile) ?: error("Failed to get a Document for UtUtils file")
        }

        unblockDocument(model.project, utUtilsDocument)
    }

    private fun overwriteUtilClass(
        existingUtilClass: PsiFile,
        utilClassKind: UtilClassKind,
        model: GenerateTestsModel,
        indicator: ProgressIndicator
    ): PsiFile {
        val utilsClassDocument = runReadAction {
            PsiDocumentManager
                .getInstance(model.project)
                .getDocument(existingUtilClass)
                ?: error("Failed to get Document for UtUtils class PsiFile: ${existingUtilClass.name}")
        }

        val utUtilsText = utilClassKind.getUtilClassText(model.codegenLanguage)

        run(EDT_LATER, indicator, "Overwrite utility class") {
            run(WRITE_ACTION, indicator, "Overwrite utility class") {
                unblockDocument(model.project, utilsClassDocument)
                executeCommand {
                    utilsClassDocument.setText(utUtilsText.replace("jdk.internal.misc", "sun.misc"))
                }
                unblockDocument(model.project, utilsClassDocument)
            }
        }
        return existingUtilClass
    }

    /**
     * This method creates an util class file and adds it into [utilClassDirectory].
     *
     * @param utilClassDirectory directory to put util class into.
     * @param language language of util class.
     * @param utilClassKind kind of util class required by the test class(es) that we generated.
     * @param model [GenerateTestsModel] that contains some useful information for util class generation.
     */
    private fun createNewUtilClass(
        utilClassDirectory: PsiDirectory,
        language: CodegenLanguage,
        utilClassKind: UtilClassKind,
        model: GenerateTestsModel,
    ): PsiFile {
        val utUtilsName = language.utilClassFileName

        val utUtilsText = utilClassKind.getUtilClassText(model.codegenLanguage)

        var utUtilsFile = runReadAction {
            PsiFileFactory.getInstance(model.project)
                .createFileFromText(
                    utUtilsName,
                    model.codegenLanguage.fileType,
                    utUtilsText
                )
        }

        // add UtUtils class file into the utils directory
        runWriteCommandAction(model.project) {
            // The file actually added to subdirectory may be the copy of original file -- see [PsiElement.add] docs
            utUtilsFile = utilClassDirectory.add(utUtilsFile) as PsiFile
        }

        return utUtilsFile
    }

    /**
     * Util class must have a comment that specifies its version.
     * This property represents the version specified by this comment if it exists. Otherwise, the property is `null`.
     */
    private val PsiFile.utilClassVersionOrNull: String?
        get() = runReadAction {
            val utilClass = (this as? PsiClassOwner)
                ?.classes
                ?.firstOrNull()
                ?: return@runReadAction null

            utilClass.getChildrenOfType<PsiComment>()
                .map { comment -> comment.text }
                .firstOrNull { text -> UtilClassKind.UTIL_CLASS_VERSION_COMMENT_PREFIX in text }
                ?.substringAfterLast(UtilClassKind.UTIL_CLASS_VERSION_COMMENT_PREFIX)
                ?.substringBefore("\n")
                ?.trim()
        }

    /**
     * Util class must have a comment that specifies its kind.
     * This property obtains the kind specified by this comment if it exists. Otherwise, the property is `null`.
     */
    private fun PsiFile.utilClassKindOrNull(codegenLanguage: CodegenLanguage): UtilClassKind?
        = runReadAction {
            val utilClass = (this as? PsiClassOwner)
                ?.classes
                ?.firstOrNull()
                ?: return@runReadAction null

            utilClass.getChildrenOfType<PsiComment>()
                .map { comment -> comment.text }
                .firstNotNullOfOrNull { text -> UtilClassKind.utilClassKindByCommentOrNull(text, codegenLanguage) }
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

    private val CodegenLanguage.utilClassFileName: String
        get() = "$UT_UTILS_INSTANCE_NAME${this.extension}"

    /**
     * @param testDirectory root test directory where we will put our generated tests.
     * @return directory for util class if it exists or null otherwise.
     */
    private fun getUtilDirectoryOrNull(
        testDirectory: PsiDirectory,
        codegenLanguage: CodegenLanguage,
    ): PsiDirectory? {
        val directoryNames = UtilClassKind.utilsPackageNames(codegenLanguage)
        var currentDirectory = testDirectory
        for (name in directoryNames) {
            val subdirectory = runReadAction { currentDirectory.findSubdirectory(name) } ?: return null
            currentDirectory = subdirectory
        }
        return currentDirectory
    }

    /**
     * @param testDirectory root test directory where we will put our generated tests.
     * @return file of util class if it exists or null otherwise.
     */
    private fun CodegenLanguage.getUtilClassOrNull(
        testDirectory: PsiDirectory,
        codegenLanguage: CodegenLanguage,
    ): PsiFile? {
        return runReadAction {
            val utilDirectory = getUtilDirectoryOrNull(testDirectory, codegenLanguage)
            utilDirectory?.findFile(this.utilClassFileName)
        }
    }

    /**
     * @param project project whose classes we generate tests for.
     * @param testModule module where the generated tests will be placed.
     * @return an existing util class from one of the test source roots
     * in the given [testModule] or `null` if no util class was found.
     */
    private fun CodegenLanguage.getUtilClassOrNull(project: Project, testModule: Module): PsiFile? {
        val psiManager = PsiManager.getInstance(project)

        // all test roots for the given test module
        val testRoots = runReadAction {
            testModule
                .suitableTestSourceRoots()
                .mapNotNull { psiManager.findDirectory(it.dir) }
        }

        // return an util class from one of the test source roots or null if no util class was found
        return testRoots.firstNotNullOfOrNull { testRoot -> getUtilClassOrNull(testRoot, this) }
    }

    /**
     * Create all package directories for UtUtils class.
     * @return the innermost directory - utils from `org.utbot.runtime.utils`
     */
    private fun createUtUtilSubdirectories(
        baseTestDirectory: PsiDirectory,
        codegenLanguage: CodegenLanguage,
    ): PsiDirectory {
        val directoryNames = UtilClassKind.utilsPackageNames(codegenLanguage)
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

    private fun waitForCountDown(latch: CountDownLatch, timeout: Long = 5, timeUnit: TimeUnit = TimeUnit.SECONDS, indicator : ProgressIndicator, action: Runnable) {
        try {
            if (!latch.await(timeout, timeUnit)) {
                run(THREAD_POOL, indicator, "Waiting for ${latch.count} sarif report(s) in a loop") {
                    waitForCountDown(latch, timeout, timeUnit, indicator, action) }
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
        proc: EngineProcess,
        testSetsId: Long,
        srcClass: PsiClass,
        classUnderTest: ClassId,
        testClass: PsiClass,
        filePointer: SmartPsiElementPointer<PsiFile>,
        srcClassPathToSarifReport: MutableMap<Path, Sarif>,
        model: GenerateTestsModel,
        reportsCountDown: CountDownLatch,
        utilClassListener: UtilClassListener,
        indicator: ProgressIndicator
    ) {
        assertIsWriteThread()
        val classMethods = srcClass.extractClassMethodsIncludingNested(false)
        val testPackageName = testClass.packageName
        val editor = CodeInsightUtil.positionCursorAtLBrace(testClass.project, filePointer.containingFile, testClass)
        //TODO: Use PsiDocumentManager.getInstance(model.project).getDocument(file)
        // if we don't want to open _all_ new files with tests in editor one-by-one
        run(THREAD_POOL, indicator, "Rendering test code") {
            val (generatedTestsCode, utilClassKind) = try {
                val paramNames = try {
                    proc.findMethodParamNames(classUnderTest, classMethods)
                } catch (e: Exception) {
                    logger.warn(e) { "Cannot find method param names for ${classUnderTest.name}" }
                    reportsCountDown.countDown()
                    return@run
                }
                proc.render(
                    model.testType,
                    testSetsId,
                    classUnderTest,
                    model.projectType,
                    paramNames.toMutableMap(),
                    generateUtilClassFile = true,
                    model.testFramework,
                    model.mockFramework,
                    model.staticsMocking,
                    model.forceStaticMocking,
                    model.generateWarningsForStaticMocking,
                    model.codegenLanguage,
                    model.parametrizedTestSource,
                    model.runtimeExceptionTestsBehaviour,
                    model.hangingTestsTimeout,
                    enableTestsTimeout = true,
                    testPackageName
                )
            } catch (e: Exception) {
                logger.warn(e) { "Cannot render test class ${testClass.name}" }
                reportsCountDown.countDown()
                return@run
            }
            utilClassListener.onTestClassGenerated(utilClassKind)
            run(EDT_LATER, indicator, "Writing generation text to documents") {
                run(WRITE_ACTION, indicator, "Writing generation text to documents") {
                    try {
                        unblockDocument(testClass.project, editor.document)
                        // TODO: JIRA:1246 - display warnings if we rewrite the file
                        executeCommand(testClass.project, "Insert Generated Tests") {
                            editor.document.setText(generatedTestsCode.replace("jdk.internal.misc.Unsafe", "sun.misc.Unsafe"))
                        }
                        unblockDocument(testClass.project, editor.document)
                    } catch (e: Exception) {
                        logger.error(e) { "Cannot save document for ${testClass.name}" }
                        reportsCountDown.countDown()
                        return@run
                    }

                    // after committing the document the `testClass` is invalid in PsiTree,
                    // so we have to reload it from the corresponding `file`
                    val testClassUpdated = (filePointer.containingFile as PsiClassOwner).classes.first() // only one class in the file

                    // reformatting before creating reports due to
                    // SarifReport requires the final version of the generated tests code
//                    run(THREAD_POOL, indicator) {
//                        IntentionHelper(model.project, editor, filePointer).applyIntentions()
                        run(EDT_LATER, indicator, "Tests reformatting") {
                            try {
                                runWriteCommandAction(filePointer.project, "UnitTestBot tests reformatting", null, {
                                    reformat(model, filePointer, testClassUpdated)
                                })
                                unblockDocument(testClassUpdated.project, editor.document)
                            } catch (e : Exception) {
                                logger.error(e) { "Cannot save Sarif report for ${testClassUpdated.name}" }
                            }
                            // uploading formatted code
                            val file = filePointer.containingFile

                            val srcClassPath = srcClass.containingFile.virtualFile.toNioPath()
                            saveSarifReport(
                                proc,
                                testSetsId,
                                testClassUpdated,
                                classUnderTest,
                                model,
                                reportsCountDown,
                                file?.text ?: generatedTestsCode,
                                srcClassPathToSarifReport,
                                srcClassPath,
                                indicator
                            )

                            unblockDocument(testClassUpdated.project, editor.document)
                        }
//                    }
                }
            }
        }
    }

    private fun reformat(model: GenerateTestsModel, smartPointer: SmartPsiElementPointer<PsiFile>, testClass: PsiClass) {
        val project = model.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        val file = smartPointer.containingFile?: return
        val fileLength = runReadAction {
            FileDocumentManager.getInstance().getDocument(file.virtualFile)?.textLength
                ?: file.virtualFile.length.toInt()
        }
        if (fileLength > UtSettings.maxTestFileSize && file.name != model.codegenLanguage.utilClassFileName) {
            CommonLoggingNotifier().notify(
                "Size of ${file.virtualFile.presentableName} exceeds configured limit " +
                        "(${FileUtil.byteCountToDisplaySize(UtSettings.maxTestFileSize.toLong())}), reformatting was skipped.",
                model.project, model.testModule, arrayOf(DumbAwareAction.create("Configure the Limit") { showSettingsEditor(model.project, "maxTestFileSize") }
                ))
            return
        }

        DumbService.getInstance(model.project).runWhenSmart {
            OptimizeImportsProcessor(project, file).run()
            codeStyleManager.reformat(file)
            when (model.codegenLanguage) {
                CodegenLanguage.JAVA -> {
                    val range = file.textRange
                    val startOffset = range.startOffset
                    val endOffset = range.endOffset
                    val reformatRange = codeStyleManager.reformatRange(file, startOffset, endOffset, false)
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(reformatRange, DO_NOT_ADD_IMPORTS)
                }
                CodegenLanguage.KOTLIN -> ShortenReferences.DEFAULT.process((testClass as KtUltraLightClass).kotlinOrigin.containingKtFile)
            }
        }
    }

    private fun saveSarifReport(
        proc: EngineProcess,
        testSetsId: Long,
        testClass: PsiClass,
        testClassId: ClassId,
        model: GenerateTestsModel,
        reportsCountDown: CountDownLatch,
        generatedTestsCode: String,
        srcClassPathToSarifReport: MutableMap<Path, Sarif>,
        srcClassPath: Path,
        indicator: ProgressIndicator
    ) {
        val project = model.project

        try {
            // saving sarif report
            SarifReportIdea.createAndSave(
                proc,
                testSetsId,
                testClassId,
                model,
                generatedTestsCode,
                testClass,
                reportsCountDown,
                srcClassPathToSarifReport,
                srcClassPath,
                indicator
            )
        } catch (e: Exception) {
            logger.error(e) { "error in saving sarif report"}
            showErrorDialogLater(
                project,
                message = "Cannot save Sarif report via generated tests: error occurred '${e.message}'",
                title = "Failed to save Sarif report"
            )
        }
    }


    private fun eventLogMessage(project: Project): String? = runReadAction {
        return@runReadAction if (ToolWindowManager.getInstance(project).getToolWindow("Event Log") != null)
            """
            <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.eventLogSuffix}">See details in Event Log</a>.
        """.trimIndent()
        else null
    }

    private fun showTestsReport(proc: EngineProcess, model: GenerateTestsModel) {
        val (notifyMessage, statistics, hasWarnings) = proc.generateTestsReport(model, eventLogMessage(model.project))

        runReadAction {
            if (hasWarnings) {
                WarningTestsReportNotifier.notify(notifyMessage)
            } else {
                TestsReportNotifier.notify(notifyMessage)
            }

            statistics?.let { DetailsTestsReportNotifier.notify(it) }
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
                    else -> { }
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

    private fun <T : Comparable<T>> maxOfNullable(a: T?, b: T?): T? {
        return when {
            a == null -> b
            b == null -> a
            else -> maxOf(a, b)
        }
    }
}
