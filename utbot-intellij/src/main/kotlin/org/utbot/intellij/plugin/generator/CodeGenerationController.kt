package org.utbot.intellij.plugin.generator

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.fileTemplates.JavaTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.util.IncorrectOperationException
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.exists
import com.siyeh.ig.psiutils.ImportUtils
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.util.ImportInsertHelperImpl
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.scripting.resolve.classId
import org.utbot.common.HTML_LINE_SEPARATOR
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.toHtmlLinkTag
import org.utbot.common.appendHtmlLine
import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.TestsCodeWithTestReport
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.intellij.plugin.generator.CodeGenerationController.Target.EDT_LATER
import org.utbot.intellij.plugin.generator.CodeGenerationController.Target.READ_ACTION
import org.utbot.intellij.plugin.generator.CodeGenerationController.Target.THREAD_POOL
import org.utbot.intellij.plugin.generator.CodeGenerationController.Target.WRITE_ACTION
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.sarif.SarifReportIdea
import org.utbot.intellij.plugin.sarif.SourceFindingStrategyIdea
import org.utbot.intellij.plugin.ui.SarifReportNotifier
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.ui.TestsReportNotifier
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import org.utbot.intellij.plugin.ui.utils.getOrCreateTestResourcesPath
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.util.signature
import org.utbot.sarif.SarifReport
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

object CodeGenerationController {
    private enum class Target { THREAD_POOL, READ_ACTION, WRITE_ACTION, EDT_LATER }

    fun generateTests(model: GenerateTestsModel, testSetsByClass: Map<PsiClass, List<UtMethodTestSet>>) {
        val baseTestDirectory = model.testSourceRoot?.toPsiDirectory(model.project)
            ?: return
        val allTestPackages = getPackageDirectories(baseTestDirectory)
        val latch = CountDownLatch(testSetsByClass.size)

        for (srcClass in testSetsByClass.keys) {
            val testSets = testSetsByClass[srcClass] ?: continue
            try {
                val classPackageName = if (model.testPackageName.isNullOrEmpty())
                    srcClass.containingFile.containingDirectory.getPackage()?.qualifiedName else model.testPackageName
                val testDirectory = allTestPackages[classPackageName] ?: baseTestDirectory
                val testClass = createTestClass(srcClass, testDirectory, model) ?: continue
                val file = testClass.containingFile
                runWriteCommandAction(model.project, "Generate tests with UtBot", null, {
                    try {
                        generateCodeAndSaveReports(testClass, file, testSets, model, latch)
                    } catch (e: IncorrectOperationException) {
                        showCreatingClassError(model.project, createTestClassName(srcClass))
                    }
                })
            } catch (e: IncorrectOperationException) {
                showCreatingClassError(model.project, createTestClassName(srcClass))
            }
        }
        run(READ_ACTION) {
            val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
            run(THREAD_POOL) {
                waitForCountDown(latch, model, sarifReportsPath)
            }
        }
    }

    private fun run(target: Target, runnable: Runnable) {
        UtContext.currentContext()?.let {
            when (target) {
                THREAD_POOL -> AppExecutorUtil.getAppExecutorService().submit {
                    withUtContext(it) {
                        runnable.run()
                    }
                }
                READ_ACTION -> runReadAction { withUtContext(it) { runnable.run() } }
                WRITE_ACTION -> runWriteAction { withUtContext(it) { runnable.run() } }
                EDT_LATER -> invokeLater { withUtContext(it) { runnable.run() } }
            }
        } ?: error("No context in thread ${Thread.currentThread()}")
    }

    private fun waitForCountDown(latch: CountDownLatch, model: GenerateTestsModel, sarifReportsPath: Path) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                run(THREAD_POOL) { waitForCountDown(latch, model, sarifReportsPath) }
            } else {
                mergeSarifReports(model, sarifReportsPath)
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
                You can open it using the VS Code extension "Sarif Viewer"
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

    private fun generateCodeAndSaveReports(
        testClass: PsiClass,
        file: PsiFile,
        testSets: List<UtMethodTestSet>,
        model: GenerateTestsModel,
        reportsCountDown: CountDownLatch,
    ) {
        val selectedMethods = TestIntegrationUtils.extractClassMethods(testClass, false)
        val testFramework = model.testFramework
        val mockito = model.mockFramework
        val staticsMocking = model.staticsMocking

        val classUnderTest = testSets.first().method.clazz

        val params = findMethodParams(classUnderTest, selectedMethods)

        val codeGenerator = CodeGenerator().apply {
            init(
                classUnderTest = classUnderTest.java,
                params = params.toMutableMap(),
                testFramework = testFramework,
                mockFramework = mockito,
                codegenLanguage = model.codegenLanguage,
                parameterizedTestSource = model.parametrizedTestSource,
                staticsMocking = staticsMocking,
                forceStaticMocking = model.forceStaticMocking,
                generateWarningsForStaticMocking = model.generateWarningsForStaticMocking,
                runtimeExceptionTestsBehaviour = model.runtimeExceptionTestsBehaviour,
                hangingTestsTimeout = model.hangingTestsTimeout,
                enableTestsTimeout = true,
                testClassPackageName = testClass.packageName
            )
        }

        val editor = CodeInsightUtil.positionCursorAtLBrace(testClass.project, file, testClass)
        //TODO: Use PsiDocumentManager.getInstance(model.project).getDocument(file)
        // if we don't want to open _all_ new files with tests in editor one-by-one
        run(THREAD_POOL) {
            val testsCodeWithTestReport = codeGenerator.generateAsStringWithTestReport(testSets)
            val generatedTestsCode = testsCodeWithTestReport.generatedCode
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
                    runWriteCommandAction(testClassUpdated.project, "UtBot tests reformatting", null, {
                        reformat(model, file, testClassUpdated)
                    })
                    unblockDocument(testClassUpdated.project, editor.document)

                    // uploading formatted code
                    val testsCodeWithTestReportFormatted =
                        testsCodeWithTestReport.copy(generatedCode = file.text)

                    // creating and saving reports
                    saveSarifAndTestReports(
                        testClassUpdated,
                        testSets,
                        model,
                        testsCodeWithTestReportFormatted,
                        reportsCountDown
                    )

                    unblockDocument(testClassUpdated.project, editor.document)
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

    private fun findMethodParams(clazz: KClass<*>, methods: List<MemberInfo>): Map<UtMethod<*>, List<String>> {
        val bySignature = methods.associate { it.signature() to it.paramNames() }
        return clazz.functions.mapNotNull { method ->
            bySignature[method.signature()]?.let { params ->
                UtMethod(method, clazz) to params
            }
        }.toMap()
    }

    private fun MemberInfo.paramNames(): List<String> =
        (this.member as PsiMethod).parameterList.parameters.map { it.name }

    private fun saveSarifAndTestReports(
        testClass: PsiClass,
        testSets: List<UtMethodTestSet>,
        model: GenerateTestsModel,
        testsCodeWithTestReport: TestsCodeWithTestReport,
        reportsCountDown: CountDownLatch
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
        } finally {
            reportsCountDown.countDown()
        }

        try {
            // Parametrized tests are not supported in tests report yet
            // TODO JIRA:1507
            if (model.parametrizedTestSource != ParametrizedTestSource.PARAMETRIZE) {
                executeCommand(testClass.project, "Saving tests report") {
                    saveTestsReport(testsCodeWithTestReport, model)
                }
            }
        } catch (e: Exception) {
            showErrorDialogLater(
                project,
                message = "Cannot save tests generation report: error occurred '${e.message}'",
                title = "Failed to save tests report"
            )
        }
    }

    private fun saveTestsReport(testsCodeWithTestReport: TestsCodeWithTestReport, model: GenerateTestsModel) {
        val testResourcesDirPath = model.testModule.getOrCreateTestResourcesPath(model.testSourceRoot)

        require(testResourcesDirPath.exists()) {
            "Test resources directory $testResourcesDirPath does not exist"
        }

        val testReportSubDir = "utbot-tests-report"
        val classFqn = with(testsCodeWithTestReport.testsGenerationReport.classUnderTest) {
            qualifiedName ?: error("Could not save tests report for anonymous or local class $this")
        }
        val fileReportPath = classFqnToPath(classFqn)

        val resultedReportedPath =
            Paths.get(
                testResourcesDirPath.toString(),
                testReportSubDir,
                fileReportPath + "TestReport" + TestsGenerationReport.EXTENSION
            )

        val parent = resultedReportedPath.parent
        requireNotNull(parent) {
            "Expected from parent of $resultedReportedPath to be not null but it is null"
        }

        VfsUtil.createDirectories(parent.toString())
        resultedReportedPath.toFile().writeText(testsCodeWithTestReport.testsGenerationReport.getFileContent())

        processInitialWarnings(testsCodeWithTestReport, model)

        val notifyMessage = buildString {
            appendHtmlLine(testsCodeWithTestReport.testsGenerationReport.toString())
            appendHtmlLine()
            val classUnderTestPackageName =
                testsCodeWithTestReport.testsGenerationReport.classUnderTest.classId.packageFqName.toString()
            if (classUnderTestPackageName != model.testPackageName) {
                val warningMessage = """
                    Warning: Destination package ${model.testPackageName} does not match package of the class $classUnderTestPackageName.
                    This may cause unnecessary usage of reflection for protected or package-private fields and methods access.
                """.trimIndent()
                appendHtmlLine(warningMessage)
                appendHtmlLine()
            }
            val savedFileMessage = """
                Tests report was saved to ${toHtmlLinkTag(resultedReportedPath.toString())} in TSV format
            """.trimIndent()
            appendHtmlLine(savedFileMessage)
        }
        TestsReportNotifier.notify(notifyMessage)
    }

    private fun processInitialWarnings(testsCodeWithTestReport: TestsCodeWithTestReport, model: GenerateTestsModel) {
        val hasInitialWarnings = model.forceMockHappened || model.forceStaticMockHappened || model.hasTestFrameworkConflict
        if (!hasInitialWarnings) {
            return
        }

        testsCodeWithTestReport.testsGenerationReport.apply {
            summaryMessage = { "Unit tests for $classUnderTest were generated with warnings.<br>" }

            if (model.forceMockHappened) {
                initialWarnings.add {
                    """
                    <b>Warning</b>: Some test cases were ignored, because no mocking framework is installed in the project.<br>
                    Better results could be achieved by <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.mockitoSuffix}">installing mocking framework</a>.
                """.trimIndent()
                }
            }
            if (model.forceStaticMockHappened) {
                initialWarnings.add {
                    """
                    <b>Warning</b>: Some test cases were ignored, because mockito-inline is not installed in the project.<br>
                    Better results could be achieved by <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.mockitoInlineSuffix}">configuring mockito-inline</a>.
                """.trimIndent()
                }
            }
            if (model.hasTestFrameworkConflict) {
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
