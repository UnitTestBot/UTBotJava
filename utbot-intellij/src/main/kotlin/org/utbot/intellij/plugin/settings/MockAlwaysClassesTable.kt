package org.utbot.intellij.plugin.settings

import org.utbot.engine.Mocker
import com.intellij.execution.util.ListTableWithButtons
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.cellvalidators.CellComponentProvider
import com.intellij.openapi.ui.cellvalidators.CellTooltipManager
import com.intellij.openapi.ui.cellvalidators.ValidatingTableCellRendererWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.util.function.BiFunction
import java.util.function.Supplier
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

@Suppress("UnstableApiUsage")
internal class MockAlwaysClassesTable(project: Project) : ListTableWithButtons<MockAlwaysClassesTable.Item>() {

    // TODO maybe use MinusculeMatcher for completion sometime

    init {
        Companion.project = project

        val table: JBTable = tableView
        table.emptyText.clear()
        table.emptyText.appendLine("Add class fully qualified name")
        table.emptyText.appendLine("to enable force static mocking for it")
        table.isStriped = false
        CellTooltipManager(project).withCellComponentProvider(CellComponentProvider.forTable(table)).installOn(table)
        Disposer.register(project, validatorsDisposable)

        project.service<Settings>().classesToMockAlways.forEach { addNewElement(Item(it)) }
    }

    override fun createListModel(): ListTableModel<Item> =
        ListTableModel(columnInfo)

    override fun createElement(): Item = Item("")

    override fun isEmpty(element: Item): Boolean = element.fullyQualifiedName.isEmpty()

    override fun cloneElement(variable: Item): Item = variable

    // we don't let delete default classes
    override fun canDeleteElement(selection: Item): Boolean =
        selection.fullyQualifiedName !in Mocker.defaultSuperClassesToMockAlwaysNames

    fun reset() {
        setValues(project.service<Settings>().classesToMockAlways.map { Item(it) })
    }

    fun apply() {
        project.service<Settings>().setClassesToMockAlways(elements.map { it.fullyQualifiedName })
    }

    fun isModified(): Boolean =
        elements.map { it.fullyQualifiedName }.toSet() != project.service<Settings>().classesToMockAlways

    companion object {

        private lateinit var project: Project

        private val columnInfo: ColumnInfo<Item, String> = object : ColumnInfo<Item, String>("Class") {
            override fun valueOf(item: Item): String = item.fullyQualifiedName

            private val validationInfoProducer = BiFunction { value: String?, component: JComponent? ->
                if (value.isNullOrEmpty()) {
                    return@BiFunction ValidationInfo("Class fully qualified name cannot be null or empty", component)
                }

                // skip any validations for out default classes
                if (value in Mocker.defaultSuperClassesToMockAlwaysNames) {
                    return@BiFunction null
                }

                checkPsiClassByName(value) ?: return@BiFunction ValidationInfo("No such class $value found", component)

                return@BiFunction null
            }

            override fun getEditor(item: Item): TableCellEditor {
                val cellEditor = ExtendableTextField()
                cellEditor.putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, java.lang.Boolean.TRUE)
                ComponentValidator(validatorsDisposable).withValidator(
                    Supplier {
                        val text = cellEditor.text
                        validationInfoProducer.apply(text, cellEditor)
                    }).andRegisterOnDocumentListener(cellEditor).installOn(cellEditor)
                return DefaultCellEditor(cellEditor)
            }

            override fun getRenderer(item: Item): TableCellRenderer? {
                val cellEditor = JTextField()
                cellEditor.putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, java.lang.Boolean.TRUE)
                return ValidatingTableCellRendererWrapper(DefaultTableCellRenderer()).withCellValidator { value, _, _ ->
                    validationInfoProducer.apply(value.toString(), null)
                }.bindToEditorSize { cellEditor.preferredSize }
            }

            override fun isCellEditable(item: Item): Boolean {
                return true
            }

            override fun setValue(item: Item, value: String) {
                item.fullyQualifiedName = value
            }
        }

        private val validatorsDisposable = Disposer.newDisposable()

        private fun checkPsiClassByName(classFullyQualifiedName: String): PsiClass? =
            JavaPsiFacade.getInstance(project).findClass(classFullyQualifiedName, GlobalSearchScope.allScope(project))
    }

    internal data class Item(var fullyQualifiedName: String) {
        override fun toString(): String = fullyQualifiedName
    }
}