package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import org.utbot.data.IdeaBuildSystem
import java.time.Duration

@FixtureName("Add File to Git Dialog")
@DefaultXpath("Dialog type", "//*[@title.key='vfs.listener.add.single.title']")
class AddFileToGitDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {

    val cancelButton
        get() = button(
            byXpath("//div[@text.key='button.cancel']"))
}