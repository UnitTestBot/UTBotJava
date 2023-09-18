package org.utbot.intellij.plugin.python

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.jetbrains.python.psi.PyClass
import org.utbot.intellij.plugin.python.language.PythonLanguageAssistant
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestSet
import org.utbot.python.utils.camelToSnakeCase
import java.nio.file.Path
import java.nio.file.Paths

class PythonIntellijProcessor(
    override val configuration: PythonTestGenerationConfig,
    val project: Project,
    val model: PythonTestLocalModel,
) : PythonTestGenerationProcessor() {
    override fun saveTests(testsCode: String) {
        invokeLater {
            runWriteAction {
                val testDir = createPsiDirectoryForTestSourceRoot(model)
                val testFileName = getOutputFileName(model)
                val testPsiFile = PsiFileFactory.getInstance(model.project)
                    .createFileFromText(testFileName, PythonLanguageAssistant.language, testsCode)
                testDir.findFile(testPsiFile.name)?.delete()
                testDir.add(testPsiFile)
                val file = testDir.findFile(testPsiFile.name)!!
                OpenFileDescriptor(project, file.virtualFile).navigate(true)
            }
        }
    }

    private fun getDirectoriesFromRoot(root: Path, path: Path): List<String> {
        if (path == root || path.parent == null)
            return emptyList()
        return getDirectoriesFromRoot(root, path.parent) + listOf(path.fileName.toString())
    }

    private fun createPsiDirectoryForTestSourceRoot(model: PythonTestLocalModel): PsiDirectory {
        val root = getContentRoot(model.project, model.file.virtualFile)
        val paths = getDirectoriesFromRoot(
            Paths.get(root.path),
            Paths.get(model.testSourceRootPath)
        )
        val rootPSI = getContainingElement<PsiDirectory>(model.file) { it.virtualFile == root }!!
        return paths.fold(rootPSI) { acc, folderName ->
            acc.findSubdirectory(folderName) ?: acc.createSubdirectory(folderName)
        }
    }

    private fun getOutputFileName(model: PythonTestLocalModel): String {
        val moduleName = model.currentPythonModule.camelToSnakeCase().replace('.', '_')
        return if (model.selectedElements.size == 1 && model.selectedElements.first() is PyClass) {
            val className = model.selectedElements.first().name?.camelToSnakeCase()?.replace('.', '_')
            "test_${moduleName}_$className.py"
        } else if (model.containingClass == null) {
            "test_$moduleName.py"
        } else {
            val className = model.containingClass.name?.camelToSnakeCase()?.replace('.', '_')
            "test_${moduleName}_$className.py"
        }
    }

    override fun notGeneratedTestsAction(testedFunctions: List<String>) {
        showErrorDialogLater(
            project,
            message = "Cannot create tests for the following functions: " + testedFunctions.joinToString(),
            title = "Python test generation error"
        )
    }

    override fun processCoverageInfo(testSets: List<PythonTestSet>) { }
}