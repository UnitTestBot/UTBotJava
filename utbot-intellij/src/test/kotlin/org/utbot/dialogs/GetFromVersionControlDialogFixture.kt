package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import org.utbot.data.IdeaBuildSystem
import java.awt.event.KeyEvent
import java.io.File

@FixtureName("Get from Version Control Dialog")
@DefaultXpath("Dialog type", "//*[@title.key='get.from.version.control']")
class GetFromVersionControlDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {

    val urlInput
        get() = textField(
            byXpath("//div[@class='BorderlessTextField']"))

    val directoryInput
        get() = textField(
            byXpath("//div[@class='ExtendableTextField']"))

    val cloneButton
        get() = button(
            byXpath("//div[@text.key='clone.dialog.clone.button']"))

    val cancelButton
        get() = button(
            byXpath("//div[@text.key='button.cancel']"))

    fun fillDialog(url: String, location: String = "") {
        urlInput.keyboard { enterText(url) }
        if (directoryInput.hasText(location).not()) { // firstly change directory, otherwise it won't be updated with project name
            directoryInput.click()
            keyboard{
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                enterText(location.replace(File.separator, File.separator + File.separator))
            }
        }
    }
}