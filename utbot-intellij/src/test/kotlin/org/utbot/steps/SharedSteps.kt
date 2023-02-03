package org.utbot.steps

import com.intellij.remoterobot.fixtures.TextEditorFixture
import com.intellij.remoterobot.fixtures.dataExtractor.contains
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.utbot.dialogs.DialogFixture
import org.utbot.dialogs.DialogFixture.Companion.byTitle
import org.utbot.pages.IdeaFrame
import java.awt.event.KeyEvent
import java.time.Duration
import java.time.Duration.ofSeconds

fun IdeaFrame.autocomplete(text: String) {
    step("Autocomplete '" + text + "'") {
        keyboard {
            enterText(text)
        }
        heavyWeightWindow(ofSeconds(5))
            .findText(contains(text))
            .click()
        keyboard {
            enter()
        }
    }
}

fun TextEditorFixture.goToLineAndColumn(row: Int, column: Int) {
    keyboard {
        if (remoteRobot.isMac()) {
            hotKey(KeyEvent.VK_META, KeyEvent.VK_L)
        } else {
            hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_G)
        }
        enterText("$row:$column")
        enter()
    }
}

fun IdeaFrame.closeTipOfTheDay() {
    step("Close Tip of the Day if it appears") {
        waitFor(ofSeconds(20)) {
            remoteRobot.findAll<DialogFixture>(byXpath("//div[@class='MyDialog'][.//div[@text='Running startup activities...']]"))
                .isEmpty()
        }
        try {
            find<DialogFixture>(byTitle ("Tip of the Day"))
                .button("Close").click()
        } catch (ignore: Throwable) {
        }
    }
}