package org.utbot.intellij.plugin.ui.utils

import com.intellij.ui.components.Panel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.LayoutBuilder
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

fun LayoutBuilder.labeled(text: String, component: JComponent) {
    row {
        cell(false) {
            label(text)
            component()
        }
    }
}

fun Cell.panelNoTitle(wrappedComponent: Component, hasSeparator: Boolean = true): CellBuilder<JPanel> {
    val panel = Panel(null, hasSeparator)
    panel.add(wrappedComponent)
    return component(panel)
}
