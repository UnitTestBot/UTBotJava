package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard

@FixtureName("UnitTestBotDialog")
@DefaultXpath("Dialog type", "//*[contains(@title, 'UnitTestBot')]")
class UnitTestBotDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {
    val keyboard: Keyboard = Keyboard(remoteRobot)

    val sdkNotificationLabel
        get() = jLabel(
            byXpath("//div[@class='SdkNotificationPanel']//div[@defaulticon='fatalError.svg']"))

    val setupSdkLink
        get() = actionLink(
            byXpath("//div[@class='SdkNotificationPanel']//div[@class='HyperlinkLabel']"))

    val testSourcesRootComboBox
        get() = comboBox(
            byXpath("//div[@class='TestFolderComboWithBrowseButton']/div[1]"))

    val generateTestsButton
        get() = button(
            byXpath("//div[@class='MainButton']"))

}