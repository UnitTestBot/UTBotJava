package org.utbot.intellij.plugin.ui.components

import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import org.utbot.framework.plugin.api.CodeGenerationSettingItem

internal class CodeGenerationSettingItemRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).apply {
            if (value is CodeGenerationSettingItem) {
                text = value.displayName
            }
        }
    }
}