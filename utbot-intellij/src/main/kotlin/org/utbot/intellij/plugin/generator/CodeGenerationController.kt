package org.utbot.intellij.plugin.generator

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.FileModificationService
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
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
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
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
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
import org.utbot.framework.codegen.model.CodeGeneratorResult
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.UtilClassKind.Companion.UT_UTILS_CLASS_NAME
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
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots
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

    private class UtilClassListener {
        var requiredUtilClassKind: UtilClassKind? = null

        fun onTestClassGenerated(result: CodeGeneratorResult) {
            requiredUtilClassKind = maxOfNullable(requiredUtilClassKind, result.utilClassKind)
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
        val utilClassListener = UtilClassListener()
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
                            utilClassListener
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
                val requiredUtilClassKind = utilClassListener.requiredUtilClassKind
                    ?: return@waitForCountDown // no util class needed

                val existingUtilClass = model.codegenLanguage.getUtilClassOrNull(model.project, model.testModule)
                val utilClassKind = newUtilClassKindOrNull(existingUtilClass, requiredUtilClassKind)
                if (utilClassKind != null) {
                    createOrUpdateUtilClass(
                        testDirectory = baseTestDirectory,
                        utilClassKind = utilClassKind,
                        existingUtilClass = existingUtilClass,
                        model = model
                    )
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
    private fun newUtilClassKindOrNull(existingUtilClass: PsiFile?, requiredUtilClassKind: UtilClassKind): UtilClassKind? {
        if (existingUtilClass == null) {
            // If no util class exists, then we should create a new one with the given kind.
            return requiredUtilClassKind
        }

        val existingUtilClassVersion = existingUtilClass.utilClassVersionOrNull ?: return requiredUtilClassKind
        val newUtilClassVersion = requiredUtilClassKind.utilClassVersion
        val versionIsUpdated = existingUtilClassVersion != newUtilClassVersion

        val existingUtilClassKind = existingUtilClass.utilClassKindOrNull ?: return requiredUtilClassKind

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
        model: GenerateTestsModel
    ) {
        val language = model.codegenLanguage

        val utUtilsFile = if (existingUtilClass == null) {
            // create a directory to put utils class into
            val utilClassDirectory = createUtUtilSubdirectories(testDirectory)
            // create util class file and put it into utils directory
            createNewUtilClass(utilClassDirectory, language, utilClassKind, model)
        } else {
            overwriteUtilClass(existingUtilClass, utilClassKind, model)
        }

        val utUtilsClass = runReadAction {
            // there's only one class in the file
            (utUtilsFile as PsiClassOwner).classes.first()
        }

        runWriteCommandAction(model.project, "UtBot util class reformatting", null, {
            reformat(model, utUtilsFile, utUtilsClass)
        })

        val utUtilsDocument = PsiDocumentManager
            .getInstance(model.project)
            .getDocument(utUtilsFile) ?: error("Failed to get a Document for UtUtils file")

        unblockDocument(model.project, utUtilsDocument)
    }

    private fun overwriteUtilClass(
        existingUtilClass: PsiFile,
        utilClassKind: UtilClassKind,
        model: GenerateTestsModel
    ): PsiFile {
        val utilsClassDocument = PsiDocumentManager
            .getInstance(model.project)
            .getDocument(existingUtilClass)
            ?: error("Failed to get Document for UtUtils class PsiFile: ${existingUtilClass.name}")

        val utUtilsText = utilClassKind.getUtilClassText(model.codegenLanguage)

        run(EDT_LATER) {
            run(WRITE_ACTION) {
                unblockDocument(model.project, utilsClassDocument)
                executeCommand {
                    utilsClassDocument.setText(utUtilsText)
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

        val utUtilsFile = runReadAction {
            PsiFileFactory.getInstance(model.project)
                .createFileFromText(
                    utUtilsName,
                    model.codegenLanguage.fileType,
                    utUtilsText
                )
        }

        // add UtUtils class file into the utils directory
        runWriteCommandAction(model.project) {
            utilClassDirectory.add(utUtilsFile)
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
                ?.trim()
        }

    /**
     * Util class must have a comment that specifies its kind.
     * This property obtains the kind specified by this comment if it exists. Otherwise, the property is `null`.
     */
    private val PsiFile.utilClassKindOrNull: UtilClassKind?
        get() = runReadAction {
            val utilClass = (this as? PsiClassOwner)
                ?.classes
                ?.firstOrNull()
                ?: return@runReadAction null

            utilClass.getChildrenOfType<PsiComment>()
                .map { comment -> comment.text }
                .mapNotNull { text -> UtilClassKind.utilClassKindByCommentOrNull(text) }
                .firstOrNull()
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
        get() = "$UT_UTILS_CLASS_NAME${this.extension}"

    /**
     * @param testDirectory root test directory where we will put our generated tests.
     * @return directory for util class if it exists or null otherwise.
     */
    private fun getUtilDirectoryOrNull(testDirectory: PsiDirectory): PsiDirectory? {
        val directoryNames = UtilClassKind.utilsPackages
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
    private fun CodegenLanguage.getUtilClassOrNull(testDirectory: PsiDirectory): PsiFile? {
        return runReadAction {
            val utilDirectory = getUtilDirectoryOrNull(testDirectory)
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
                .suitableTestSourceRoots(this)
                .mapNotNull { psiManager.findDirectory(it) }
        }

        // return an util class from one of the test source roots or null if no util class was found
        return testRoots
            .mapNotNull { testRoot -> getUtilClassOrNull(testRoot) }
            .firstOrNull()
    }

    /**
     * Create all package directories for UtUtils class.
     * @return the innermost directory - utils from `org.utbot.runtime.utils`
     */
    private fun createUtUtilSubdirectories(baseTestDirectory: PsiDirectory): PsiDirectory {
        val directoryNames = UtilClassKind.utilsPackages
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
        utilClassListener: UtilClassListener
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
            utilClassListener.onTestClassGenerated(codeGenerationResult)
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
        testsCodeWithTestReport: CodeGeneratorResult,
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

    private fun <T : Comparable<T>> maxOfNullable(a: T?, b: T?): T? {
        return when {
            a == null -> b
            b == null -> a
            else -> maxOf(a, b)
        }
    }
}
