package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.utbot.data.JDKVersion
import java.time.Duration

@FixtureName("Project Structure Dialog")
@DefaultXpath("Dialog type", "//*[@title.key='project.settings.display.name']")
class ProjectStructureDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {

    val projectJdkCombobox
        get() = comboBox(
            byXpath("//div[@class='JdkComboBox']"))

    val moduleSdkCombobox
        get() = comboBox(
            byXpath("//div[@text.key='module.libraries.target.jdk.module.radio']/../div[@class='JdkComboBox']"))

    val okButton
        get() = button(
            byXpath("//div[@text.key='button.ok']"))

    fun setProjectSdk(jdkVersion: JDKVersion) {
        findText("Project").click()
        projectJdkCombobox.click()
        waitForIgnoringError(Duration.ofSeconds(5)) {
            heavyWeightWindow().itemsList.isShowing
        }
        heavyWeightWindow().itemsList.clickItem(jdkVersion.namePart, fullMatch = false)
    }

}