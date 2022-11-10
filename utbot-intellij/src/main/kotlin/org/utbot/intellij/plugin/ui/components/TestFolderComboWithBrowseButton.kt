package org.utbot.intellij.plugin.ui.components

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.FixedSizeButton
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.UIUtil
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import org.jetbrains.kotlin.idea.util.rootManager
import org.utbot.common.PathUtil
import org.utbot.intellij.plugin.generator.CodeGenerationController.getAllTestSourceRoots
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.utils.TestSourceRoot
import org.utbot.intellij.plugin.ui.utils.addDedicatedTestRoot
import org.utbot.intellij.plugin.ui.utils.dedicatedTestSourceRootName
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle

class TestFolderComboWithBrowseButton(private val model: GenerateTestsModel) :
    ComponentWithBrowseButton<ComboBox<Any>>(ComboBox(), null) {

    private val SET_TEST_FOLDER = "set test folder"

    init {
        if (model.project.isBuildWithGradle) {
            setButtonEnabled(false)
            UIUtil.findComponentOfType(this, FixedSizeButton::class.java)?.toolTipText = "Please define custom test source root via Gradle"
        }
        childComponent.isEditable = false
        childComponent.renderer = object : ColoredListCellRenderer<Any?>() {
            override fun customizeCellRenderer(
                list: JList<out Any?>,
                value: Any?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value is String) {
                    append(value)
                    return
                }
                if (value is VirtualFile) {
                    append(formatUrl(value, model))
                }
                if (value is FakeVirtualFile) {
                    append(" (will be created)", SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
            }
        }

        val testRoots = model.getSortedTestRoots()

        // this method is blocked for Gradle, where multiple test modules can exist
        model.testModule.addDedicatedTestRoot(testRoots, model.codegenLanguage)

        if (testRoots.isNotEmpty()) {
            configureRootsCombo(testRoots)
        } else {
            newItemList(setOf(SET_TEST_FOLDER))
        }

        addActionListener {
            val testSourceRoot = chooseTestRoot(model)
            testSourceRoot?.let {
                model.setSourceRootAndFindTestModule(it)

                if (childComponent.itemCount == 1 && childComponent.selectedItem == SET_TEST_FOLDER) {
                    newItemList(setOf(it))
                } else {
                    //Prepend and select newly added test root
                    val testRootItems = linkedSetOf(it)
                    testRootItems += (0 until childComponent.itemCount).map { i -> childComponent.getItemAt(i) as VirtualFile}
                    newItemList(testRootItems)
                }
            }
        }
    }

    private fun GenerateTestsModel.getSortedTestRoots(): MutableList<TestSourceRoot> {
        var commonModuleSourceDirectory = ""
        for ((i, sourceRoot) in srcModule.rootManager.sourceRoots.withIndex()) {
            commonModuleSourceDirectory = if (i == 0) {
                sourceRoot.toNioPath().toString()
            } else {
                StringUtil.commonPrefix(commonModuleSourceDirectory, sourceRoot.toNioPath().toString())
            }
        }

        return getAllTestSourceRoots().distinct().toMutableList().sortedWith(
            compareByDescending<TestSourceRoot> {
                // Heuristics: Dirs with proper code language should go first
                it.expectedLanguage == codegenLanguage
            }.thenByDescending {
                // Heuristics: Dirs from within module 'common' directory should go first
                it.dir.toNioPath().toString().startsWith(commonModuleSourceDirectory)
            }.thenByDescending {
                // Heuristics: dedicated test source root named 'utbot_tests' should go first
                it.dir.name == dedicatedTestSourceRootName
            }.thenBy {
                // ABC-sorting
                it.dir.toNioPath()
            }
        ).toMutableList()
    }

    private fun chooseTestRoot(model: GenerateTestsModel): VirtualFile? =
        ReadAction.compute<VirtualFile, RuntimeException> {
            val desc = object:FileChooserDescriptor(false, true, false, false, false, false) {
                override fun isFileSelectable(file: VirtualFile?): Boolean {
                    return file != null && ModuleUtil.findModuleForFile(file, model.project) != null && super.isFileSelectable(file)
                }
            }
            val initialFile = model.project.guessProjectDir()

            val files = FileChooser.chooseFiles(desc, model.project, initialFile)
            files.singleOrNull()
        }

    private fun configureRootsCombo(testRoots: List<TestSourceRoot>) {
        val selectedRoot = testRoots.first()

        // do not update model.testModule here, because fake test source root could have been chosen
        model.testSourceRoot = selectedRoot.dir
        newItemList(testRoots.map { it.dir }.toSet())
    }

    private fun newItemList(comboItems: Set<Any>) {
        childComponent.model = DefaultComboBoxModel(ArrayUtil.toObjectArray(comboItems))
    }

    private fun formatUrl(virtualFile: VirtualFile, model: GenerateTestsModel): String {
        var directoryUrl = if (virtualFile is FakeVirtualFile) {
            virtualFile.parent.presentableUrl + File.separatorChar + virtualFile.name
        } else {
            virtualFile.presentableUrl
        }
        @Suppress("DEPRECATION")
        val projectHomeUrl = model.project.baseDir.presentableUrl

        PathUtil.safeRelativize(projectHomeUrl, directoryUrl)
            ?.let { directoryUrl = ".../$it" }

        return directoryUrl
    }
}