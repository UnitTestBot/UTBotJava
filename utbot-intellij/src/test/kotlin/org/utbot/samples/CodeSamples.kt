package org.utbot.samples

import com.intellij.remoterobot.fixtures.TextEditorFixture
import com.intellij.remoterobot.utils.keyboard
import org.utbot.data.TEST_RUN_NUMBER
import java.awt.event.KeyEvent

fun TextEditorFixture.typeAdditionFunction(className: String): String {
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
    return "@utbot.returnsFrom {@code return a + b;}"
}

fun TextEditorFixture.typeDivisionFunction(className: String) : String {
    editor.selectText(className)
    keyboard {
        key(KeyEvent.VK_END)
        enter()
        enterText("public int division(")
        enterText("int a, int b")
        key(KeyEvent.VK_END)
        enterText("{")
        enter()
        enterText("// ${TEST_RUN_NUMBER}")
        enter()
        enterText("return a / b;")
    }
    return "@utbot.returnsFrom {@code return a / b;}"
}
