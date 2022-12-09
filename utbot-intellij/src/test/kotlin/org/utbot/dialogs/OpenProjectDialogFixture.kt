package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath

@FixtureName("OpenProjectDialog")
@DefaultXpath("Dialog type", "//*[@title.key='title.open.file.or.project']")
class OpenProjectDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {

    val pathInput
        get() = textField(
            byXpath("//div[@class='BorderlessTextField']"))

    val okButton
        get() = button(
            byXpath("//div[@text.key='button.ok']"))
}