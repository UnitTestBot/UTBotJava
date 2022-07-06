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
import org.utbot.common.PathUtil
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.intellij.plugin.ui.utils.BaseTestsModel
import org.utbot.intellij.plugin.ui.utils.addDedicatedTestRoot
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots

class TestFolderComboWithBrowseButton(private val model: BaseTestsModel) : ComboboxWithBrowseButton() {

    private val SET_TEST_FOLDER = "set test folder"

    init {
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

        val testRoots = model.testModule.suitableTestSourceRoots(CodegenLanguage.JAVA).toMutableList()
        model.testModule.addDedicatedTestRoot(testRoots)
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

    private fun createNewTestSourceRoot(model: BaseTestsModel): VirtualFile? =
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
        newItemList(testRoots.toSet())
    }

    private fun newItemList(comboItems: Set<Any>) {
        childComponent.model = DefaultComboBoxModel(ArrayUtil.toObjectArray(comboItems))
    }

    private fun formatUrl(virtualFile: VirtualFile, model: BaseTestsModel): String {
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