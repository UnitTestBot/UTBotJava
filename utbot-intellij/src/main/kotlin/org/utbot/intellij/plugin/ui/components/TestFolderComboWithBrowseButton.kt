package org.utbot.intellij.plugin.ui.components

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ArrayUtil
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.utbot.common.PathUtil
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.utils.addDedicatedTestRoot
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots

class TestFolderComboWithBrowseButton(private val model: GenerateTestsModel) : ComboboxWithBrowseButton() {

    private val SET_TEST_FOLDER = "set test folder"

    init {
        if (model.project.isBuildWithGradle) {
            setButtonEnabled(false)
            button.toolTipText = "Please define custom test source root via Gradle"
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

        val suggestedModules =
            if (model.project.isBuildWithGradle) model.project.allModules() else model.potentialTestModules

        val testRoots = suggestedModules.flatMap { it.suitableTestSourceRoots().toList() }.toMutableList()
        // this method is blocked for Gradle, where multiple test modules can exist
        model.testModule.addDedicatedTestRoot(testRoots)

        if (testRoots.isNotEmpty()) {
            configureRootsCombo(testRoots)
        } else {
            newItemList(setOf(SET_TEST_FOLDER))
        }

        addActionListener {
            val testSourceRoot = createNewTestSourceRoot(model)
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

    private fun createNewTestSourceRoot(model: GenerateTestsModel): VirtualFile? =
        ReadAction.compute<VirtualFile, RuntimeException> {
            val desc = FileChooserDescriptor(false, true, false, false, false, false)
            val initialFile = model.project.guessProjectDir()

            val files = FileChooser.chooseFiles(desc, model.project, initialFile)
            files.singleOrNull()
        }

    private fun configureRootsCombo(testRoots: List<VirtualFile>) {
        // unfortunately, Gradle creates Kotlin test source root with Java source root type, so type is misleading
        val selectedRoot = testRoots.first()

        // do not update model.testModule here, because fake test source root could have been chosen
        model.testSourceRoot = selectedRoot
        newItemList(testRoots.toSet())
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