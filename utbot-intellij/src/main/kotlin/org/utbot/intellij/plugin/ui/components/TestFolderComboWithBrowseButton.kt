package org.utbot.intellij.plugin.ui.components

import org.utbot.common.PathUtil
import org.utbot.intellij.plugin.ui.GenerateTestsModel
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.ArrayUtil
import javax.swing.DefaultComboBoxModel

class TestFolderComboWithBrowseButton(private val model: GenerateTestsModel) : ComboboxWithBrowseButton() {

    private val SET_TEST_FOLDER = "set test folder"

    init {
        childComponent.isEditable = false

        val testRoots = model.testModule.suitableTestSourceRoots()

        if (testRoots.isNotEmpty()) {
            configureRootsCombo(testRoots)
        } else {
            newItemList(setOf(SET_TEST_FOLDER))
        }

        addActionListener {
            val testSourceRoot = createNewTestSourceRoot(model)
            testSourceRoot?.let {
                model.testSourceRoot = it

                if (childComponent.itemCount == 1 && childComponent.selectedItem == SET_TEST_FOLDER) {
                    newItemList(setOf(formatUrl(it, model)))
                } else {
                    //Prepend and select newly added test root
                    val testRootItems = linkedSetOf(formatUrl(it, model))
                    testRootItems += (0 until childComponent.itemCount).map { i -> childComponent.getItemAt(i) as String}
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

        model.testSourceRoot = selectedRoot
        newItemList(testRoots.map { root -> formatUrl(root, model) }.toSet())
    }

    private fun newItemList(comboItems: Set<String>) {
        childComponent.model = DefaultComboBoxModel(ArrayUtil.toObjectArray(comboItems))
        childComponent.putClientProperty("JComponent.outline",
                if (comboItems.isNotEmpty() && !comboItems.contains(SET_TEST_FOLDER)) null else DarculaUIUtil.Outline.error)
    }

    private fun formatUrl(virtualFile: VirtualFile, model: GenerateTestsModel): String {
        var directoryUrl = virtualFile.presentableUrl
        @Suppress("DEPRECATION")
        val projectHomeUrl = model.project.baseDir.presentableUrl

        PathUtil.safeRelativize(projectHomeUrl, directoryUrl)
            ?.let { directoryUrl = ".../$it" }

        return directoryUrl
    }
}