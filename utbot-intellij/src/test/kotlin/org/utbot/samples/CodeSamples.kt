package org.utbot.samples

import com.intellij.remoterobot.fixtures.TextEditorFixture
import com.intellij.remoterobot.utils.keyboard
import org.utbot.data.TEST_RUN_NUMBER
import java.awt.event.KeyEvent

fun TextEditorFixture.additionFunction(className: String) {
    editor.selectText(className)
    keyboard {
        key(KeyEvent.VK_END)
        enter()
        enterText("public int addition(")
        enterText("int a, int b")
        key(KeyEvent.VK_END)
        enterText("{")
        enter()
        enterText("// UTBot UI ${TEST_RUN_NUMBER} test")
        enter()
        enterText("return a + b;")
    }
}
