package org.utbot.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.dialogs.GetFromVersionControlDialogFixture
import org.utbot.dialogs.NewProjectDialogFixture
import org.utbot.dialogs.OpenOrImportProjectDialogFixture
import org.utbot.dialogs.OpenProjectDialogFixture
import java.io.File
import java.time.Duration

fun RemoteRobot.welcomeFrame(function: WelcomeFrame.()-> Unit) {
    find(WelcomeFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Welcome Frame")
@DefaultXpath("Welcome Frame type", "//div[@class='FlatWelcomeFrame']")
class WelcomeFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {
    val keyboard: Keyboard = Keyboard(remoteRobot)
    val ideaVersion //ToDO good locator
        get() = jLabel(byXpath("//div[@class='TabbedWelcomeScreen']//div[@class='JLabel' and contains(@text,'20')]"))

    val newProjectLink
        get() = actionLink(byXpath("New Project","//div[(@class='MainButton' and @text='New Project') or (@accessiblename='New Project' and @class='JButton')]"))
    val openProjectLink
        get() = actionLink(byXpath("Open","//div[(@class='MainButton' and @text='Open') or (@accessiblename.key='action.WelcomeScreen.OpenProject.text')]"))
    val getFromVSCLink
        get() = actionLink(byXpath("Get from VCS","//div[(@class='MainButton' and @text='Get from VCS')]"))
    val moreActions
        get() = button(byXpath("More Action", "//div[@accessiblename='More Actions']"))

    val recentProjectLinks
        get() = jTree(byXpath("//div[@class='CardLayoutPanel']//div[@class='Tree']"))

    val newProjectDialog
        get() = remoteRobot.find(NewProjectDialogFixture::class.java)

    val openProjectDialog
        get() = remoteRobot.find(OpenProjectDialogFixture::class.java)

    val getFromVersionControlDialog
        get() = remoteRobot.find(GetFromVersionControlDialogFixture::class.java)

    val openOrImportProjectDialog
        get() = remoteRobot.find(OpenOrImportProjectDialogFixture::class.java,
            Duration.ofSeconds(120))

    fun openProjectByPath(location: String, projectName: String = "") {
        val separator = File.separator
        val localPath = location + separator + projectName
        openProjectLink.click()
        keyboard.enterText(localPath.replace(separator, separator + separator))
        openProjectDialog.okButton.click()
    }

    fun createNewProject(projectName: String, location: String = "", locationPart: String = "",
                         language: String = "Java", buildSystem: IdeaBuildSystem = IdeaBuildSystem.INTELLIJ,
                         jdkVersion: JDKVersion, addSampleCode: Boolean = true) {
        newProjectLink.click()
        newProjectDialog.selectWizard("New Project")
        newProjectDialog.fillDialog(
            projectName, location, locationPart, language,
            buildSystem, jdkVersion, addSampleCode
        )
        newProjectDialog.createButton.click()
    }

    fun cloneProjectFromVC(url: String, location: String = "",
                           buildSystem: IdeaBuildSystem) {
        getFromVSCLink.click()
        getFromVersionControlDialog.fillDialog(url, location)
        getFromVersionControlDialog.cloneButton.click()
        openOrImportProjectDialog.selectBuildSystem(buildSystem)
    }
}
