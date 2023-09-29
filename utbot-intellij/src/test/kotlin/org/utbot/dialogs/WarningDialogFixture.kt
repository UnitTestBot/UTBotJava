package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard

@FixtureName("MyDialog")
@DefaultXpath("type", "//div[@class='DialogRootPane']")
class WarningDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {
    val keyboard: Keyboard = Keyboard(remoteRobot)

    val terminateButton
        get() = button(
            byXpath("//div[@text.key='button.terminate']"))

    val cancelButton
        get() = button(
            byXpath("//div[@text.key='button.cancel']"))

    val proceedButton
        get() = button(
            byXpath("//div[@text='Proceed']"))

    val goBackButton
        get() = button(
            byXpath("//div[@text='Go Back']"))

}