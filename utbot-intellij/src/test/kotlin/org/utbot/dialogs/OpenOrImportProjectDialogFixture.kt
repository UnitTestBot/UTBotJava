package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import org.utbot.data.IdeaBuildSystem
import java.time.Duration

@FixtureName("Open or Import Project Dialog")
@DefaultXpath("Dialog type", "//*[@title.key='project.open.select.from.multiple.processors.dialog.title']")
class OpenOrImportProjectDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {

    val openAsRadioButtons
        get() = radioButtons(
            byXpath("//div[@class='JBRadioButton']"))

    val okButton
        get() = button(
            byXpath("//div[@text.key='button.ok']"))

    val cancelButton
        get() = button(
            byXpath("//div[@text.key='button.cancel']"))

    fun selectBuildSystem(buildSystem: IdeaBuildSystem) {
        waitFor {
            openAsRadioButtons.isNotEmpty()
        }
        openAsRadioButtons.filter {
            it.text.contains(buildSystem.system)
        }[0].click()
        okButton.click()
    }
}