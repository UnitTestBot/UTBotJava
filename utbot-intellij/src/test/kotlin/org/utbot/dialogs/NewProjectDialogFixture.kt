package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.Keyboard
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import java.awt.event.KeyEvent
import java.time.Duration
import java.time.Duration.ofSeconds

@FixtureName("NewProjectDialog")
@DefaultXpath("type", "//*[contains(@title.key, 'title.new.project')]")
class NewProjectDialogFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent)
    : DialogFixture(remoteRobot, remoteComponent) {
    val keyboard: Keyboard = Keyboard(remoteRobot)

    val wizardsList
        get() = jList(
            byXpath("//div[@class='JBList']"))

    val nameInput
        get() = textField(
            byXpath("//div[@class='JBTextField']"))

    val locationInput
        get() = textField(
            byXpath("//div[@class='ExtendableTextField']"))

    val addSampleCodeCheckbox
        get() = checkBox(
            byXpath("//div[@text.key='label.project.wizard.new.project.add.sample.code']"))

    val jdkComboBox
        get() = comboBox(
            byXpath("//div[@class='JdkComboBox']"),
            Duration.ofSeconds(10))

    val jdkList
        get() = heavyWeightWindow().itemsList

    val createButton
        get() = button(
            byXpath("//div[@text.key='button.create']"))

    val cancelButton
        get() = button(
            byXpath("//div[@text.key='button.cancel']"))

    fun selectWizard(wizardName: String) {
        if (title != wizardName) {
            wizardsList.findText(wizardName).click()
        }
    }

    fun selectJDK(jdkVersion: String) {
        step("Select JDK: $jdkVersion") {
            jdkComboBox.click()
            waitForIgnoringError(ofSeconds(20)) {
                findAll<ComponentFixture>(byXpath("//*[@text.key='progress.title.detecting.sdks']")).isEmpty()
            }
            val jdkMatching = jdkList.collectItems().first { it.contains(jdkVersion) }
            jdkList.clickItem(jdkMatching)
        }
    }

    fun fillDialog(projectName: String,
                   location: String = "",
                   language: String = "Java",
                   buildSystem: IdeaBuildSystem = IdeaBuildSystem.INTELLIJ,
                   jdkVersion: JDKVersion,
                   addSampleCode: Boolean = true) {
        step("Fill New Project dialog") {
            nameInput.doubleClick()
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
            keyboard.enterText(projectName)
            var input = "D:\\JavaProjects\\Autotests"
            if (location != "") {
                input = location
            }
            if (locationInput.hasText(input).not()) {
                locationInput.doubleClick()
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                keyboard.enterText(location.replace("\\", "\\\\"))
            }
            this.findText(language).click()
            this.findText(buildSystem.system).click()
            addSampleCodeCheckbox.setValue(addSampleCode)
            if (!jdkComboBox.selectedText().contains(jdkVersion.namePart)) {
                selectJDK(jdkVersion.namePart)
            }
        }
    }
}