package org.utbot.intellij.plugin.generator

import org.utbot.common.HTML_LINE_SEPARATOR
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.toHtmlLinkTag
import org.utbot.common.appendHtmlLine
import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.TestsCodeWithTestReport
import org.utbot.framework.codegen.model.ModelBasedTestCodeGenerator
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.intellij.plugin.sarif.SarifReportIdea
import org.utbot.intellij.plugin.sarif.SourceFindingStrategyIdea
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import org.utbot.intellij.plugin.ui.utils.getOrCreateTestResourcesPath
import org.utbot.sarif.SarifReport
import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.fileTemplates.JavaTemplateUtil
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
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
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.util.IncorrectOperationException
import com.intellij.util.io.exists
import com.siyeh.ig.psiutils.ImportUtils
import java.nio.file.Paths
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
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
import org.utbot.intellij.plugin.error.showErrorDialogLater
import org.utbot.intellij.plugin.ui.GenerateTestsModel
import org.utbot.intellij.plugin.ui.SarifReportNotifier
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.ui.TestsReportNotifier
import org.utbot.intellij.plugin.ui.packageName

object TestGenerator {
    fun generateTests(model: GenerateTestsModel, testCases: Map<PsiClass, List<UtTestCase>>) {
        runWriteCommandAction(model.project, "Generate tests with UtBot", null, {
            generateTestsInternal(model, testCases)
        })
    }

    private fun generateTestsInternal(model: GenerateTestsModel, testCasesByClass: Map<PsiClass, List<UtTestCase>>) {
        val baseTestDirectory = model.testSourceRoot?.toPsiDirectory(model.project)
            ?: return
        val allTestPackages = getPackageDirectories(baseTestDirectory)

        for (srcClass in testCasesByClass.keys) {
            val testCases = testCasesByClass[srcClass] ?: continue
            try {
                val classPackageName = if (model.testPackageName.isNullOrEmpty())
                    srcClass.containingFile.containingDirectory.getPackage()?.qualifiedName else model.testPackageName
                val testDirectory = allTestPackages[classPackageName] ?: baseTestDirectory
                val testClass = createTestClass(srcClass, testDirectory, model.codegenLanguage) ?: continue
                val file = testClass.containingFile

                addTestMethodsAndSaveReports(testClass, file, testCases, model)
            } catch (e: IncorrectOperationException) {
                showCreatingClassError(model.project, createTestClassName(srcClass))
            }
        }

        mergeSarifReports(model)
    }

    private fun mergeSarifReports(model: GenerateTestsModel) {
        val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
        val sarifReports = sarifReportsPath.toFile()
            .walkTopDown()
            .filter { it.extension == "sarif" }
            .map { it.readText() }
            .toList()

        val mergedReport = SarifReport.mergeReports(sarifReports)
        val mergedReportPath = sarifReportsPath.resolve("${model.project.name}Report.sarif")
        mergedReportPath.toFile().writeText(mergedReport)

        // notifying the user
        SarifReportNotifier.notify(
            info = """
                SARIF report was saved to ${toHtmlLinkTag(mergedReportPath.toString())}$HTML_LINE_SEPARATOR
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

    private fun createTestClass(srcClass: PsiClass, testDirectory: PsiDirectory, codegenLanguage: CodegenLanguage): PsiClass? {
        val testClassName = createTestClassName(srcClass)
        val aPackage = JavaDirectoryService.getInstance().getPackage(testDirectory)

        if (aPackage != null) {
            val scope = GlobalSearchScopesCore.directoryScope(testDirectory, false)

            // Here we use firstOrNull(), because by some unknown reason
            // findClassByShortName() may return two identical objects.
            // Be careful, do not use singleOrNull() here, because it expects
            // the array to contain strictly one element and otherwise returns null.
            aPackage.findClassByShortName(testClassName, scope)
                .firstOrNull {
                    when (codegenLanguage) {
                        CodegenLanguage.JAVA ->  it !is KtUltraLightClass
                        CodegenLanguage.KOTLIN -> it is KtUltraLightClass
                    }
                }?.let {
                    return if (FileModificationService.getInstance().preparePsiElementForWrite(it)) it else null
                }
        }

        val fileTemplate = FileTemplateManager.getInstance(testDirectory.project).getInternalTemplate(
            when (codegenLanguage) {
                CodegenLanguage.JAVA -> JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME
                CodegenLanguage.KOTLIN -> "Kotlin Class"
            }
        )
        val createFromTemplate: PsiElement = FileTemplateUtil.createFromTemplate(
            fileTemplate,
            testClassName,
            FileTemplateManager.getInstance(testDirectory.project).defaultProperties,
            testDirectory
        )

        return (createFromTemplate.containingFile as PsiClassOwner).classes.first()
    }

    private fun addTestMethodsAndSaveReports(
        testClass: PsiClass,
        file: PsiFile,
        testCases: List<UtTestCase>,
        model: GenerateTestsModel,
    ) {
        val selectedMethods = TestIntegrationUtils.extractClassMethods(testClass, false)
        val testFramework = model.testFramework
        val mockito = model.mockFramework
        val staticsMocking = model.staticsMocking

        val classUnderTest = testCases.first().method.clazz

        val params = findMethodParams(classUnderTest, selectedMethods)

        val generator = model.project.service<Settings>().codeGenerator.apply {
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
                testClassPackageName = testClass.packageName
            )
        }

        when (generator) {
            is ModelBasedTestCodeGenerator -> {
                val editor = CodeInsightUtil.positionCursorAtLBrace(testClass.project, file, testClass)
                val testsCodeWithTestReport = generator.generateAsStringWithTestReport(testCases)
                val generatedTestsCode = testsCodeWithTestReport.generatedCode

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
                reformat(model, file, testClassUpdated)
                unblockDocument(testClassUpdated.project, editor.document)

                // uploading formatted code
                val testsCodeWithTestReportFormatted = testsCodeWithTestReport.copy(generatedCode = file.text)

                // creating and saving reports
                saveSarifAndTestReports(testClassUpdated, testCases, model, testsCodeWithTestReportFormatted)

                unblockDocument(testClassUpdated.project, editor.document)
            }
            else -> TODO("Only model based code generator supported, but got: ${generator::class}")
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


    private fun saveSarifAndTestReports(
        testClass: PsiClass,
        testCases: List<UtTestCase>,
        model: GenerateTestsModel,
        testsCodeWithTestReport: TestsCodeWithTestReport
    ) {
        val project = model.project
        val generatedTestsCode = testsCodeWithTestReport.generatedCode

        try {
            // saving sarif report
            val sourceFinding = SourceFindingStrategyIdea(testClass)
            executeCommand(testClass.project, "Saving Sarif report") {
                SarifReportIdea.createAndSave(model, testCases, generatedTestsCode, sourceFinding)
            }
        } catch (e: Exception) {
            showErrorDialogLater(
                project,
                message = "Cannot save Sarif report via generated tests: error occurred '${e.message}'",
                title = "Failed to save Sarif report"
            )
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
            Paths.get(testResourcesDirPath.toString(), testReportSubDir, fileReportPath + "TestReport" + TestsGenerationReport.EXTENSION)

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
            val classUnderTestPackageName = testsCodeWithTestReport.testsGenerationReport.classUnderTest.classId.packageFqName.toString()
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
        TestsReportNotifier.notify(notifyMessage, model.project, model.testModule)
    }

    private fun processInitialWarnings(testsCodeWithTestReport: TestsCodeWithTestReport, model: GenerateTestsModel) {
        val hasInitialWarnings = model.forceMockHappened || model.hasTestFrameworkConflict
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
