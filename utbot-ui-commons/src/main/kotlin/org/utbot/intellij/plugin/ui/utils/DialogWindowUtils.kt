package org.utbot.intellij.plugin.ui.utils

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.utbot.framework.codegen.domain.TestFramework
import javax.swing.JList

fun createTestFrameworksRenderer(additionalText: String): ColoredListCellRenderer<TestFramework> {
    return object : ColoredListCellRenderer<TestFramework>() {
        override fun customizeCellRenderer(
            list: JList<out TestFramework>, value: TestFramework,
            index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            this.append(value.displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (!value.isInstalled) {
                this.append(additionalText, SimpleTextAttributes.ERROR_ATTRIBUTES)
            }
        }
    }
}
